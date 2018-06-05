import unittest.mock
import asyncio
import python.Protobuf.protobuf_utils as putils
from python.Socket.SocketLayer import SocketLayer
from python.Message.MessageLayer import MessageLayer
from python.P2P.P2PLayer import P2PLayer
from python.Business.BusinessLogicLayer import BusinessLogicLayer
from python.Protobuf.Message_pb2 import Message
from python.P2P.peer import Peer
from python.utils.StatusMessage import StatusMessage
import python.Business.util as file_util

def _run(cor):
    return asyncio.get_event_loop().run_until_complete(cor)

class BusinessLayerTest(unittest.TestCase):

    def setUp(self):
        ml = unittest.mock.MagicMock()
        p2pl = P2PLayer(ml, "127.0.0.1", 8080)
        self.business_layer = BusinessLogicLayer(p2pl)
        q3 = asyncio.Queue()
        q4 = asyncio.Queue()
        self.lower = (q3, q4)

        _run(self.business_layer.add_layer_communication(
            self.lower
        ))

    def tearDown(cls):
        pending = asyncio.Task.all_tasks()
        for task in pending:
            task.cancel()
            asyncio.get_event_loop().run_until_complete(task)

    @classmethod
    def tearDownClass(cls):
        asyncio.get_event_loop().close()

    def test_setting_up_ilc(self):
        self.assertIs(self.business_layer._lower, self.lower)

    def test_ping(self):
        """
        Business layer creates ping message using protobuf_utils
        and passes it on to the lower layer
        """
        peer = Peer(2, "127.33.21.22", 3233, False)
        _run(self.business_layer.lower_layer._routing_table.insert(peer))
        status = _run(self.business_layer.ping(2))
        self.assertIs(status, StatusMessage.SUCCESS)
        message = _run(self.lower[1].get())
        self.assertIsInstance(message, Message)
        self.assertEqual(message.type, Message.PING)

        ping_message = putils.create_ping_message(self.business_layer.get_myself(), peer)
        ping_message.uuid = message.uuid
        self.assertEqual(message, ping_message)

        status = _run(self.business_layer.ping(266))
        self.assertIs(status, StatusMessage.FAILURE)

    def test_removal_of_peer_after_not_responding(self):
        peer = Peer(2, "127.33.21.22", 3233, False)
        _run(self.business_layer.lower_layer._routing_table.insert(peer))
        status = _run(self.business_layer.ping(2))
        self.assertIs(status, StatusMessage.SUCCESS)
        _run(asyncio.sleep(10))
        peer = _run(self.business_layer.lower_layer.get_peer_by_id(2))
        self.assertIs(peer, None)

    def test_that_responsive_peer_is_not_removed(self):
        peer = Peer(666, "127.33.21.22", 3233, False)
        _run(self.business_layer.lower_layer._routing_table.insert(peer))
        status = _run(self.business_layer.ping(666))
        self.assertIs(status, StatusMessage.SUCCESS)

        ping_respond_message = putils.create_ping_response_message(sender=peer, receiver=self.business_layer.get_myself())
        _run(self.lower[0].put(ping_respond_message))
        _run(asyncio.sleep(3.2))
        peer_from_routing_table = _run(self.business_layer.lower_layer.get_peer_by_id(666))
        self.assertEqual(peer, peer_from_routing_table)

    def test_find_node(self):
        """
        Business layer creates find node message using protobuf_utils
        and passes it on to the lower layer.
        """
        peer = Peer(2, "127.33.21.22", 3233, False)
        _run(self.business_layer.lower_layer._routing_table.insert(peer))
        status = _run(self.business_layer.find_node(2332, 2))
        self.assertIs(status, StatusMessage.SUCCESS)

        message = _run(self.lower[1].get())
        self.assertIsInstance(message, Message)
        self.assertEqual(message.type, Message.FIND_NODE)

        find_node_message = putils.create_find_node_message(sender=self.business_layer.get_myself(), receiver=peer, guid=2332)
        find_node_message.uuid = message.uuid
        self.assertEqual(message, find_node_message)

        status = _run(self.business_layer.find_node(guid=266, id_of_peer_to_ask=88))
        self.assertIs(status, StatusMessage.FAILURE)

    def test_handling_found_nodes(self):
        p1 = Peer(1, "123.32.33.22", 90, False)
        p2 = Peer(2, "11.22.33.22", 99, False)
        p3 = Peer(3, "34.23.42.33", 80, False)
        p4 = Peer(4, "23.44.23.21", 77, False)
        peers = [p1, p2, p3, p4]
        sender = Peer(6, "1.1.1.1", 666, False)
        this_peer = self.business_layer.get_myself()
        message = putils.create_found_nodes_message(sender=sender, receiver=this_peer, nearest_peers=peers)
        _run(self.lower[0].put(message))

        ping_respond_message1 = putils.create_ping_response_message(sender=p1, receiver=self.business_layer.get_myself())
        ping_respond_message2 = putils.create_ping_response_message(sender=p2, receiver=self.business_layer.get_myself())
        ping_respond_message3 = putils.create_ping_response_message(sender=p3, receiver=self.business_layer.get_myself())
        ping_respond_message4 = putils.create_ping_response_message(sender=p4, receiver=self.business_layer.get_myself())

        _run(self.lower[0].put(ping_respond_message1))
        _run(self.lower[0].put(ping_respond_message2))
        _run(self.lower[0].put(ping_respond_message3))
        _run(self.lower[0].put(ping_respond_message4))

        _run(asyncio.sleep(4))

        inputed_p1 = _run(self.business_layer.lower_layer.get_peer_by_id(1))
        inputed_p2 = _run(self.business_layer.lower_layer.get_peer_by_id(2))
        inputed_p3 = _run(self.business_layer.lower_layer.get_peer_by_id(3))
        inputed_p4 = _run(self.business_layer.lower_layer.get_peer_by_id(4))

        self.assertEqual(p1, inputed_p1)
        self.assertEqual(p2, inputed_p2)
        self.assertEqual(p3, inputed_p3)
        self.assertEqual(p4, inputed_p4)

    def test_sending_command(self):
        peer = Peer(5343, "127.33.21.21", 3233, False)
        _run(self.business_layer.lower_layer._routing_table.insert(peer))
        status = _run(self.business_layer.command(target_id=5343, command="ls -l", should_respond=False))
        self.assertIs(status, StatusMessage.SUCCESS)

        message = _run(self.lower[1].get())
        self.assertIsInstance(message, Message)
        self.assertEqual(message.type, Message.COMMAND)

        find_node_message = putils.create_command_message(sender=self.business_layer.get_myself(), receiver=peer,
                                                            command="ls -l", should_respond=False)
        find_node_message.uuid = message.uuid
        self.assertEqual(message, find_node_message)

        status = _run(self.business_layer.command(target_id=266, command="dir .", should_respond=False))
        self.assertIs(status, StatusMessage.FAILURE)

    # it goes well but dir gets timestamps and they don't match
    @unittest.skip
    def test_responding_to_command(self):
        another_peer = Peer(5, "127.33.21.22", 3233, False)
        this_peer = self.business_layer.get_myself()
        message = putils.create_command_message(sender=another_peer, receiver=this_peer, command="dir", should_respond=True)

        _run(self.lower[0].put(message))

        response_message = _run(self.lower[1].get())

        with open('./dir.txt', 'r', encoding='utf-8', errors='ignore') as file:
            value = file.read()
        self.assertEqual(response_message.response.value, value)
        self.assertEqual(response_message.response.status, 0)
        self.assertEqual(response_message.response.command, 'dir')

    def test_responding_to_ping(self):
        another_peer = Peer(5, "127.33.21.22", 3233, False)
        this_peer = self.business_layer.get_myself()
        message = putils.create_ping_message(sender=another_peer, receiver=this_peer)
        message2 = putils.create_ping_message(sender=another_peer, receiver=this_peer)
        message3 = putils.create_ping_message(sender=another_peer, receiver=this_peer)

        # Put three messages in queue
        _run(self.lower[0].put(message))
        _run(self.lower[0].put(message2))
        _run(self.lower[0].put(message3))

        # Three response messages should be created and passed to the lower layer
        respond_message1 = _run(self.lower[1].get())
        respond_message2 = _run(self.lower[1].get())
        respond_message3 = _run(self.lower[1].get())

        # Their uuid should be different
        self.assertNotEqual(respond_message1.uuid, respond_message2.uuid)
        self.assertNotEqual(respond_message2.uuid, respond_message3.uuid)
        self.assertNotEqual(respond_message1.uuid, respond_message3.uuid)

        # But after chaanging it to be the same for all three messages they should be the same
        respond_message2.uuid = respond_message1.uuid
        respond_message3.uuid = respond_message2.uuid
        self.assertEqual(respond_message1, respond_message2)
        self.assertEqual(respond_message1, respond_message3)
        self.assertEqual(respond_message3, respond_message2)

        inputed_peer = _run(self.business_layer.lower_layer.get_peer_by_id(5))
        self.assertEqual(respond_message1.type, Message.PING_RESPONSE)

        self.assertEqual(int(respond_message1.receiver.guid), 5)
        self.assertEqual(respond_message1.receiver.IP, "127.33.21.22")
        self.assertEqual(respond_message1.receiver.port, 3233)
        self.assertEqual(respond_message1.receiver.isNAT, False)

        self.assertEqual(int(respond_message1.sender.guid), this_peer.id)
        self.assertEqual(respond_message1.sender.IP, this_peer.ip)
        self.assertEqual(respond_message1.sender.port, this_peer.port)
        self.assertEqual(respond_message1.sender.isNAT, this_peer.is_NAT)

        self.assertEqual(inputed_peer, another_peer)

    def test_responding_to_file_request_message(self):
        another_peer = Peer(22, "127.33.21.22", 3233, False)
        message = putils.create_file_request_message(self.business_layer.get_myself(), receiver=another_peer, path='./test_file.txt')
        _run(self.lower[0].put(message))

        response_message = _run(self.lower[1].get())
        file_size = file_util.get_file_size('./test_file.txt')
        binary_data = file_util.get_file_binary_data('./test_file.txt')

        self.assertEqual(response_message.fileChunk.fileName, './test_file.txt' + '.{}'.format(self.business_layer.get_myself().id))
        self.assertEqual(response_message.fileChunk.fileSize, file_size)
        self.assertEqual(response_message.fileChunk.ordinal, 0)
        self.assertEqual(response_message.fileChunk.data, binary_data)

    @unittest.skip
    def test_handling_file_chunks_message(self):
        print(file_util.get_file_size('./huge_file.txt'))
        another_peer = Peer(22, "127.33.21.22", 3233, False)
        message = putils.create_file_request_message(self.business_layer.get_myself(), receiver=another_peer,
                                                     path='./huge_file.txt')
        _run(self.lower[0].put(message))

        chunk1 = _run(self.lower[1].get())
        chunk2 = _run(self.lower[1].get())

        _run(self.lower[0].put(chunk1))
        _run(self.lower[0].put(chunk2))

        _run(asyncio.sleep(4))

        true_binary_data = file_util.get_file_binary_data('./huge_file.txt')
        binary_data = file_util.get_file_binary_data(path='./huge.file.txt.{}'.format(self.business_layer.get_myself().id))
        self.assertEqual(binary_data, true_binary_data)

    def test_responding_to_file_request_message_for_huge_file(self):

        print(file_util.get_file_size('./huge_file.txt'))
        another_peer = Peer(22, "127.33.21.22", 3233, False)
        message = putils.create_file_request_message(self.business_layer.get_myself(), receiver=another_peer,
                                                     path='./huge_file.txt')
        _run(self.lower[0].put(message))

        chunk1 = _run(self.lower[1].get())
        chunk2 = _run(self.lower[1].get())
        file_size = file_util.get_file_size('./huge_file.txt')
        binary_data1 = file_util.get_file_binary_data('./huge_file.txt', 8192)
        binary_data2 = file_util.get_file_binary_data('./huge_file.txt')[8192:]

        self.assertEqual(chunk1.fileChunk.fileName, './huge_file.txt' + '.{}'.format(self.business_layer.get_myself().id))
        self.assertEqual(chunk1.fileChunk.fileSize, file_size)
        self.assertEqual(chunk1.fileChunk.ordinal, 0)
        self.assertEqual(chunk1.fileChunk.data, binary_data1)
        self.assertEqual(chunk2.fileChunk.fileName, './huge_file.txt' + '.{}'.format(self.business_layer.get_myself().id))
        self.assertEqual(chunk2.fileChunk.fileSize, file_size)
        self.assertEqual(chunk2.fileChunk.ordinal, 1)
        self.assertEqual(chunk2.fileChunk.data, binary_data2)
        self.assertEqual(chunk1.fileChunk.uuid, chunk2.fileChunk.uuid)

    @unittest.skip
    def test_join_network(self):
        """
        BLL calls join_network on message layer,
        and on success calls callback if any and after that returns success
        """
        mess_to_sock_queue2 = asyncio.Queue()
        sock_to_mess_queue2 = asyncio.Queue()
        mess_to_p2p_queue2 = asyncio.Queue()
        p2p_to_mess_queue2 = asyncio.Queue()
        bll_to_p2p_queue2 = asyncio.Queue()
        p2p_to_bll_queue2 = asyncio.Queue()

        socket_layer2 = SocketLayer()
        _run(socket_layer2.add_layer_communication(higher=(mess_to_sock_queue2, sock_to_mess_queue2)))

        message_layer2 = MessageLayer(socket_layer2)
        _run(message_layer2.add_layer_communication(higher=(p2p_to_mess_queue2, mess_to_p2p_queue2),
                                              lower=(sock_to_mess_queue2, mess_to_sock_queue2)))

        p2p_layer2 = P2PLayer(message_layer2, "127.0.0.2", 8090)
        _run(p2p_layer2.add_layer_communication(higher=(bll_to_p2p_queue2, p2p_to_bll_queue2),
                                          lower=(mess_to_p2p_queue2, p2p_to_mess_queue2)))

        business_logic_layer2 = BusinessLogicLayer(p2p_layer2)
        _run(business_logic_layer2.add_layer_communication(lower=(p2p_to_bll_queue2, bll_to_p2p_queue2)))

        status = _run(business_logic_layer2.join_network(("127.0.0.1", 8080)))
        self.assertIs(status, StatusMessage.SUCCESS)

        _run(asyncio.sleep(0.1))
        self.assertEqual(len(business_logic_layer2.lower_layer._routing_table), 2)
        buckets = business_logic_layer2.lower_layer._routing_table.buckets
        peers = [peer for bucket in buckets for peer in bucket]
        self.assertEqual(peers[0].ip, "127.0.0.1")
        self.assertEqual(peers[0].port, 8888)
        self.assertEqual(peers[0].is_NAT, False)
        self.assertEqual(peers[1].ip, "127.0.0.1")
        self.assertEqual(peers[1].port, 8080)
        self.assertEqual(peers[1].is_NAT, False)

class UtilsTest(unittest.TestCase):
    def test_get_binary_data(self):
        file_binary_data = file_util.get_file_binary_data("./test_file.txt")
        with open('./test_file.txt', 'rb') as file:
            file_data = file.read()

        self.assertEqual(file_binary_data, file_data)

    def test_write_binary_data(self):
        data = b'0101101011010100100101010'
        file_util.write_file_from_binary_data(data=data, path='./test_file2')

        file_binary_data = file_util.get_file_binary_data("./test_file2")

        self.assertEqual(file_binary_data, b'0101101011010100100101010')

        data = file_util.get_file_binary_data('./test_file.txt')
        file_util.write_file_from_binary_data(data=data, path='./test_file2')
        data2 = file_util.get_file_binary_data('./test_file2')
        self.assertEqual(data2, data)

if __name__ == '__main__':
    unittest.main()