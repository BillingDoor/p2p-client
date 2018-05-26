import unittest
import python.peer
import socket
import threading
import python.Message_pb2
import python.peer
import python.BucketList
import time
import python.protobuf_utils as putils
from python.kademlia_node import KademliaNode

# TODO remove
def setup_server1():
    sock = socket.socket()
    sock.bind(("127.0.0.1", 8080))
    sock.listen(5)
    ad, h = sock.accept()
    sock.close()
    print("Accepted" + str(ad))
    #Recieve find_node message
    _ = ad.recv(12000)

    msg = python.Message_pb2.Message()
    msg.type = msg.FOUND_NODES
    p1_proto = msg.pFoundNodes.nodes.add()
    p2_proto = msg.pFoundNodes.nodes.add()


    p1 = python.peer.Peer("123.22.33.11", 33, 1)
    p2 = python.peer.Peer("22.22.22.22", 11, 2)
    p1_proto.guid = p1.id
    p1_proto.IP = p1.host
    p1_proto.Port = str(p1.port)
    p1_proto.isNAT = False

    p2_proto.guid = p2.id
    p2_proto.IP = p2.host
    p2_proto.Port = str(p2.port)
    p2_proto.isNAT = False

    ad.send(msg.SerializeToString())
    ad.close()


class TestPeers(unittest.TestCase):
    def test_equality(self):
        p1 = python.peer.Peer("127.0.0.1", 22, 12)
        p2 = python.peer.Peer("127.0.0.1", 22, 12)
        p3 = python.peer.Peer("127.1.0.1", 22 , 12)
        self.assertEqual(p1, p2)
        self.assertNotEqual(p1, p3)
        self.assertNotEqual(p2, p3)
        self.assertEqual(p1, p1)
        self.assertEqual(p3, p3)

        l = [p2]
        self.assertIn(p1, l)
        self.assertNotIn(p3, l)


class TestMessaging(unittest.TestCase):
    def test_ping(self):
        k1 = KademliaNode("127.0.0.1", 8080)

        msg = putils.create_ping_message(123)

        sock = socket.socket()
        sock.connect(("127.0.0.1", 8080))
        sock.send(msg)

        response = sock.recv(12000)
        message = putils.read_message(response)
        sock.close()

        # Dunno how to do other way
        time.sleep(2)
        all_peers = [peer for bucket in k1.routing_table.buckets for peer in bucket]
        self.assertEqual(all_peers[0].id, 123)
        self.assertEqual(message.uuid, k1.peer.id)

    def test_find_node(self):
        test_node = KademliaNode("127.0.0.1", 8081, 123)
        BL = python.BucketList.BucketList(5, 64, 123)
        BL.insert(python.peer.Peer("127.11.11.1", 1, 0b01010101))
        BL.insert(python.peer.Peer("127.66.1.1", 33, 0b00110101))
        BL.insert(python.peer.Peer("127.0.9.1", 22, 0b00100101))
        BL.insert(python.peer.Peer("127.3.0.1", 5252523, 0b01010111))
        BL.insert(python.peer.Peer("125.0.0.1", 434, 0b10110101))
        BL.insert(python.peer.Peer("127.55.0.1", 2235, 0b00111101))
        BL.insert(python.peer.Peer("127.0.23.1", 123, 0b00011101))
        test_node.routing_table = BL

        test_peer = python.peer.Peer("127.0.0.1", 8081)
        found_nodes = test_peer.find_node(0b00010101)
        nodes = putils.get_peers_from_found_nodes_message(found_nodes)
        self.assertEqual(len(found_nodes.pFoundNodes.nodes), 5)
        self.assertIn(python.peer.Peer("127.0.23.1", 123, 0b00011101), nodes)
        self.assertIn(python.peer.Peer("127.66.1.1", 33, 0b00110101), nodes)
        self.assertIn(python.peer.Peer("127.0.9.1", 22, 0b00100101), nodes)
        self.assertIn(python.peer.Peer("127.55.0.1", 2235, 0b00111101), nodes)
        self.assertIn(python.peer.Peer("127.66.1.1", 33, 0b00110101), nodes)

class TestBucketList(unittest.TestCase):
    def test_get_item(self):
        BL = python.BucketList.BucketList(15, 10, 0)

        BL.insert(python.peer.Peer("127.11.11.1", 1, 0b01010101))
        BL.insert(python.peer.Peer("127.66.1.1", 33, 0b00110101))
        BL.insert(python.peer.Peer("127.0.9.1", 22, 0b00100101))
        BL.insert(python.peer.Peer("127.3.0.1", 5252523, 0b01010111))
        BL.insert(python.peer.Peer("125.0.0.1", 434, 0b10110101))
        BL.insert(python.peer.Peer("127.55.0.1", 2235, 0b00111101))
        BL.insert(python.peer.Peer("127.0.23.1", 123, 0b00011101))

        self.assertEqual(BL[0b00110101].get_info(), ("127.66.1.1", 33, 0b00110101))
        self.assertEqual(BL[32323], None)

    def test_nearest_nodes(self):
        BL = python.BucketList.BucketList(15, 10, 0)
        key_peer= python.peer.Peer("127.0.0.1", 33, 0b00010101)
        BL.insert(key_peer)

        BL.insert(python.peer.Peer("127.0.0.1", 33, 0b01010101))
        BL.insert(python.peer.Peer("127.0.0.1", 33, 0b00110101))
        BL.insert(python.peer.Peer("127.0.0.1", 33, 0b00100101))
        BL.insert(python.peer.Peer("127.0.0.1", 33, 0b01010111))
        BL.insert(python.peer.Peer("127.0.0.1", 33, 0b10110101))
        BL.insert(python.peer.Peer("127.0.0.1", 33, 0b00111101))
        BL.insert(python.peer.Peer("127.0.0.1", 33, 0b00011101))


        nodes = BL.nearest_nodes(key_peer.id, 1)
        self.assertEqual(nodes[0], python.peer.Peer("127.0.0.1", 33, 0b00010101))

        nodes = BL.nearest_nodes(key_peer.id, 4)
        self.assertIn(python.peer.Peer("127.0.0.1", 33, 0b00010101), nodes)
        self.assertIn(python.peer.Peer("127.0.0.1", 33, 0b00110101), nodes)
        self.assertIn(python.peer.Peer("127.0.0.1", 33, 0b00111101), nodes)
        self.assertIn(python.peer.Peer("127.0.0.1", 33, 0b00011101), nodes)




if __name__ == '__main__':
    unittest.main()