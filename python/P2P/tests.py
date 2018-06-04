import unittest.mock
import asyncio

import python.Protobuf.protobuf_utils as putils
from python.P2P.P2PLayer import P2PLayer
from python.Message.MessageLayer import MessageLayer
from python.P2P.peer import Peer


def _run(cor):
    return asyncio.get_event_loop().run_until_complete(cor)

class P2PTest(unittest.TestCase):

    @classmethod
    def setUpClass(self):
        sl = unittest.mock.MagicMock()
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

    def test_add_peer(self):
        peer = Peer(999, "83.34.65.32", 90, False)
        _run(self.p2pl.add_peer(peer))
        self.assertIs(peer, _run(self.p2pl.get_peer_by_id(999)))

    def test_remove_peer(self):
        peer = Peer(1234, "23.32.45.22", 9033, False)
        _run(self.p2pl.add_peer(peer))
        self.assertIs(peer, _run(self.p2pl.get_peer_by_id(1234)))

        _run(self.p2pl.remove_peer(peer))
        self.assertIs(None, _run(self.p2pl.get_peer_by_id(1234)))


    def test_passing_message(self):
        """
        P2P layer got some message from higher layer.
        It passes it on to the lower layer using queue.
        """
        sender = Peer(123, '123.123.123.123', 8022, True)
        receiver = Peer(11, '66.22.66.22', 9090, False)
        mess = putils.create_ping_message(sender, receiver)
        _run(self.higher[0].put(mess))
        # wait a while for message to propagate through the layer
        _run(asyncio.sleep(0.1))
        self.assertEqual(_run(self.lower[1].get()), mess)


if __name__ == '__main__':
    unittest.main()
