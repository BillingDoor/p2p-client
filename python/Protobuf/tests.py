import unittest.mock
import python.Protobuf.protobuf_utils as putils
from python.P2P.peer import Peer
from python.Protobuf.Message_pb2 import Message

class ProtobufTest(unittest.TestCase):
    def setUp(self):
        self.sender = Peer(123, '123.123.123.123', 8022, True)
        self.receiver = Peer(11, '66.22.66.22', 9090, False)

    def test_creation_of_basic_message(self):
        sender = self.sender
        receiver = self.receiver
        mess = putils._prepare_base_message(sender, receiver)

        self.assertEqual(mess.sender.guid, '123')
        self.assertEqual(mess.sender.IP, '123.123.123.123')
        self.assertEqual(mess.sender.port, 8022)
        self.assertEqual(mess.sender.isNAT, True)

        self.assertEqual(mess.receiver.guid, '11')
        self.assertEqual(mess.receiver.IP, '66.22.66.22')
        self.assertEqual(mess.receiver.port, 9090)
        self.assertEqual(mess.receiver.isNAT, False)

    def test_creation_of_ping_message(self):
        mess = putils.create_ping_message(self.sender, self.receiver)
        self.assertEqual(mess.type, Message.PING)

    def test_creation_of_find_node_message(self):
        mess = putils.create_find_node_message(self.sender, self.receiver, 22)
        self.assertEqual(mess.type, Message.FIND_NODE)
        self.assertEqual(mess.findNode.guid, '22')

    def test_deserializing_multiple_messages_bytes_stream(self):
        mess = putils.create_find_node_message(self.sender, self.receiver, 55)
        mess2 = putils.create_find_node_message(self.sender, self.receiver, 33)
        serialized = putils.serialize_message(mess)
        serialized2 = putils.serialize_message(mess2)

        self.assertEqual(mess2, putils.deserialize_message(serialized + serialized2))


    def test_creation_of_found_nodes_message(self):
        p1 = Peer(1, "123.32.33.22", 90, False)
        p2 = Peer(2, "11.22.33.22", 99, False)
        p3 = Peer(3, "34.23.42.33", 80, False)
        p4 = Peer(4, "23.44.23.21", 77, False)
        peers = [p1, p2, p3, p4]
        mess = putils.create_found_nodes_message(self.sender, self.receiver, peers)

        self.assertEqual(mess.type, Message.FOUND_NODES)
        self.assertEqual(len(mess.foundNodes.nodes), 4)
        self.assertEqual(putils.create_peer_from_contact(mess.foundNodes.nodes[0]), p1)
        self.assertEqual(putils.create_peer_from_contact(mess.foundNodes.nodes[1]), p2)
        self.assertEqual(putils.create_peer_from_contact(mess.foundNodes.nodes[2]), p3)
        self.assertEqual(putils.create_peer_from_contact(mess.foundNodes.nodes[3]), p4)

    def test_encoding_and_decoding(self):
        mess = putils.create_ping_message(self.sender, self.receiver)
        self.assertEqual(putils.deserialize_message(putils.serialize_message(mess)), mess)

    def test_get_receiver_address(self):
        mess = putils.create_ping_message(self.sender, self.receiver)
        self.assertEqual((self.receiver.ip, self.receiver.port), putils.get_receiver_address(mess))

if __name__ == '__main__':
    unittest.main()
