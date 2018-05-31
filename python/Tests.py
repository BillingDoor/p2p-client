import unittest
import python.peer
import socket
import python.Protobuf.Message_pb2
import python.peer
import python.P2P.BucketList
import time
import python.protobuf_utils as putils
from python.kademlia_node import KademliaNode

class TestPeers(unittest.TestCase):
    def test_equality(self):
        p1 = python.peer.Peer("127.0.0.1", 22, 12)
        p2 = python.peer.Peer("127.0.0.1", 22, 12)
        p3 = python.peer.Peer("127.1.0.1", 22, 12)
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

        msg = putils.create_ping_message(123, "127.0.0.1", 8081)

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
        self.assertEqual(all_peers[0].host, "127.0.0.1")
        self.assertEqual(all_peers[0].port, 8081)

        self.assertEqual(message.uuid, k1.peer.id)

    def test_find_node(self):
        test_node = KademliaNode("127.0.0.1", 8081, 123)
        b_l = python.P2P.BucketList.BucketList(5, 64, 123)
        b_l.insert(python.peer.Peer("127.11.11.1", 1, 0b01010101))
        b_l.insert(python.peer.Peer("127.66.1.1", 33, 0b00110101))
        b_l.insert(python.peer.Peer("127.0.9.1", 22, 0b00100101))
        b_l.insert(python.peer.Peer("127.3.0.1", 5252523, 0b01010111))
        b_l.insert(python.peer.Peer("125.0.0.1", 434, 0b10110101))
        b_l.insert(python.peer.Peer("127.55.0.1", 2235, 0b00111101))
        b_l.insert(python.peer.Peer("127.0.23.1", 123, 0b00011101))
        test_node.routing_table = b_l

        test_peer = python.peer.Peer("127.0.0.1", 8081, 12)
        found_nodes = test_peer.find_node(0b00010101, test_peer.host, test_peer.port)
        nodes = putils.get_peers_from_found_nodes_message(found_nodes)
        self.assertEqual(len(found_nodes.pFoundNodes.nodes), 5)
        self.assertIn(python.peer.Peer("127.0.23.1", 123, 0b00011101), nodes)
        self.assertIn(python.peer.Peer("127.66.1.1", 33, 0b00110101), nodes)
        self.assertIn(python.peer.Peer("127.0.9.1", 22, 0b00100101), nodes)
        self.assertIn(python.peer.Peer("127.55.0.1", 2235, 0b00111101), nodes)
        self.assertIn(python.peer.Peer("127.66.1.1", 33, 0b00110101), nodes)

        self.assertIn(test_peer, test_node.routing_table)

    @unittest.skip
    def test_bootstraping(self):
        k1 = KademliaNode("127.0.0.1", 9080, 11)
        print("k1.routingtable: {}".format(id(k1.routing_table)))
        k2 = KademliaNode("127.0.0.1", 9081, 12, seeds=[("127.0.0.1", 9080, 11)])
        time.sleep(2)
        #k3 = KademliaNode("127.0.0.1", 9082, 13, seeds=[("127.0.0.1", 9080, 11)])
        time.sleep(2)
        #k4 = KademliaNode("127.0.0.1", 9083, 14, seeds=[("127.0.0.1", 9080, 11)])

        #k2.routing_table[11].find_node(k2.peer.id)
        time.sleep(3)
        self.assertEqual(len(k1.routing_table), 3)
        self.assertIn(python.peer.Peer("127.0.0.1", 9081, 12), k1.routing_table)
        #self.assertIn(k3.peer, k1.routing_table)
        #self.assertIn(k4.peer, k1.routing_table)

        self.assertIn(k3.peer, k2.routing_table)
        self.assertIn(k2.peer, k3.routing_table)

        #self.assertIn(k4.peer, k2.routing_table)
        #self.assertIn(k4.peer, k3.routing_table)

class TestBucketList(unittest.TestCase):
    def test_get_item(self):
        b_l = python.P2P.BucketList.BucketList(15, 10, 0)

        b_l.insert(python.peer.Peer("127.11.11.1", 1, 0b01010101))
        b_l.insert(python.peer.Peer("127.66.1.1", 33, 0b00110101))
        b_l.insert(python.peer.Peer("127.0.9.1", 22, 0b00100101))
        b_l.insert(python.peer.Peer("127.3.0.1", 5252523, 0b01010111))
        b_l.insert(python.peer.Peer("125.0.0.1", 434, 0b10110101))
        b_l.insert(python.peer.Peer("127.55.0.1", 2235, 0b00111101))
        b_l.insert(python.peer.Peer("127.0.23.1", 123, 0b00011101))

        self.assertEqual(b_l[0b00110101].get_info(), ("127.66.1.1", 33, 0b00110101))
        self.assertEqual(b_l[32323], None)

    def test_contains(self):
        b_l = python.P2P.BucketList.BucketList(15, 10, 0)

        b_l.insert(python.peer.Peer("127.11.11.1", 1, 0b01010101))
        b_l.insert(python.peer.Peer("127.66.1.1", 33, 0b00110101))
        b_l.insert(python.peer.Peer("127.0.9.1", 22, 0b00100101))
        b_l.insert(python.peer.Peer("127.3.0.1", 5252523, 0b01010111))
        b_l.insert(python.peer.Peer("125.0.0.1", 434, 0b10110101))
        b_l.insert(python.peer.Peer("127.55.0.1", 2235, 0b00111101))
        b_l.insert(python.peer.Peer("127.0.23.1", 123, 0b00011101))

        self.assertIn(python.peer.Peer("127.0.9.1", 22, 0b00100101), b_l)

    def test_len(self):
        b_l = python.P2P.BucketList.BucketList(15, 10, 0)

        b_l.insert(python.peer.Peer("127.11.11.1", 1, 0b01010101))
        self.assertEqual(len(b_l), 1)
        b_l.insert(python.peer.Peer("127.66.1.1", 33, 0b00110101))
        self.assertEqual(len(b_l), 2)
        b_l.insert(python.peer.Peer("127.0.9.1", 22, 0b00100101))
        self.assertEqual(len(b_l), 3)
        b_l.insert(python.peer.Peer("127.3.0.1", 5252523, 0b01010111))
        self.assertEqual(len(b_l), 4)
        b_l.insert(python.peer.Peer("125.0.0.1", 434, 0b10110101))
        self.assertEqual(len(b_l), 5)
        b_l.insert(python.peer.Peer("127.55.0.1", 2235, 0b00111101))
        self.assertEqual(len(b_l), 6)
        b_l.insert(python.peer.Peer("127.0.23.1", 123, 0b00011101))
        self.assertEqual(len(b_l), 7)
    def test_nearest_nodes(self):
        b_l = python.P2P.BucketList.BucketList(15, 10, 0)
        key_peer = python.peer.Peer("127.0.0.1", 33, 0b00010101)
        b_l.insert(key_peer)

        b_l.insert(python.peer.Peer("127.0.0.1", 33, 0b01010101))
        b_l.insert(python.peer.Peer("127.0.0.1", 33, 0b00110101))
        b_l.insert(python.peer.Peer("127.0.0.1", 33, 0b00100101))
        b_l.insert(python.peer.Peer("127.0.0.1", 33, 0b01010111))
        b_l.insert(python.peer.Peer("127.0.0.1", 33, 0b10110101))
        b_l.insert(python.peer.Peer("127.0.0.1", 33, 0b00111101))
        b_l.insert(python.peer.Peer("127.0.0.1", 33, 0b00011101))

        nodes = b_l.nearest_nodes(key_peer.id, 1)
        self.assertEqual(nodes[0], python.peer.Peer("127.0.0.1", 33, 0b00010101))

        nodes = b_l.nearest_nodes(key_peer.id, 4)
        self.assertIn(python.peer.Peer("127.0.0.1", 33, 0b00010101), nodes)
        self.assertIn(python.peer.Peer("127.0.0.1", 33, 0b00110101), nodes)
        self.assertIn(python.peer.Peer("127.0.0.1", 33, 0b00111101), nodes)
        self.assertIn(python.peer.Peer("127.0.0.1", 33, 0b00011101), nodes)

if __name__ == '__main__':
    unittest.main()