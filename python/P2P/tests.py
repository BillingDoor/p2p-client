import unittest.mock
import asyncio

from python.Socket.SocketLayer import SocketLayer
import python.Protobuf.protobuf_utils as putils
from python.P2P.P2PLayer import P2PLayer
from python.Message.MessageLayer import MessageLayer
from python.StatusMessage import StatusMessage
from python.P2P.peer import Peer

def _run(cor):
    return asyncio.get_event_loop().run_until_complete(cor)

class P2PTest(unittest.TestCase):

    @classmethod
    def setUpClass(self):
        sl = SocketLayer()
        ml =  MessageLayer(sl)
        self.p2pl = P2PLayer(ml, "127.0.0.1", 6666, 123)
        q1 = asyncio.Queue()
        q2 = asyncio.Queue()
        q3 = asyncio.Queue()
        q4 = asyncio.Queue()
        self.higher = (q1, q2)
        self.lower = (q3, q4)
        _run(self.p2pl.add_layer_communication(higher=self.higher,
                                          lower=self.lower))

    @classmethod
    def tearDownClass(self):
        pending = asyncio.Task.all_tasks()
        for task in pending:
            task.cancel()
            asyncio.get_event_loop().run_until_complete(task)

    def test_setting_up_ilc(self):
        self.assertIs(self.p2pl._higher, self.higher)
        self.assertIs(self.p2pl._lower, self.lower)

    def test_ping(self):
        """
        P2P layer creates ping message using protobuf_utils
        and calls send() on MessageLayer
        """
        peer = Peer(2, "127.33.21.22", 3233, False)
        _run(self.p2pl._routing_table.insert(peer))
        status = _run(self.p2pl.ping(2))
        self.assertIs(status, StatusMessage.SUCCESS)

        status = _run(self.p2pl.ping(33))
        self.assertIs(status, StatusMessage.FAILURE)

    def test_get_myself(self):
        myself = self.p2pl.get_myself()
        self.assertEqual(myself.id, 123)
        self.assertEqual(myself.ip, "127.0.0.1")
        self.assertEqual(myself.port, 6666)
        self.assertEqual(myself.is_NAT, False)

    def test_get_peer(self):
        peer = Peer(1, "127.33.21.22", 3233, False)
        _run(self.p2pl._routing_table.insert(peer))

        peer = _run(self.p2pl.get_peer_by_id(1))
        self.assertEqual(peer.id, 1)
        self.assertEqual(peer.ip, "127.33.21.22")
        self.assertEqual(peer.port, 3233)
        self.assertEqual(peer.is_NAT, False)


if __name__ == '__main__':
    unittest.main()
