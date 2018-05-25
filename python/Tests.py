import unittest
import python.peer
import socket
import threading
import python.Message_pb2
import python.peer
import python.BucketList

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

@unittest.skip
class TestMessaging(unittest.TestCase):

    def test_find_node(self):
        test_peer = python.peer.Peer("127.0.0.1", 8080)

        s_th = threading.Thread(target=setup_server1)
        s_th.start()

        found_nodes = test_peer.find_node(22)

        p1 = found_nodes.pFoundNodes.nodes[0]
        p2 = found_nodes.pFoundNodes.nodes[1]
        self.assertEqual(p1.guid, 1)
        self.assertEqual(p1.IP, "123.22.33.11")
        self.assertEqual(p1.Port, "33")

        self.assertEqual(p2.guid, 2)
        self.assertEqual(p2.IP, "22.22.22.22")
        self.assertEqual(p2.Port, "11")

class TestBucketList(unittest.TestCase):
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