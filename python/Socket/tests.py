import unittest.mock
import asyncio
from python.Socket.SocketLayer import SocketLayer
from python.Protobuf.protobuf_utils import *

def _run(cor):
    return asyncio.get_event_loop().run_until_complete(cor)

class SocketsTests(unittest.TestCase):
    def setUp(self):
        self.socket_layer = SocketLayer()

    def test_setting_up_ilc(self):
        q1 = asyncio.Queue()
        q2 = asyncio.Queue()
        q3 = asyncio.Queue()
        q4 = asyncio.Queue()
        higher = (q1, q2)
        lower = (q3, q4)

        _run(self.socket_layer.add_layer_communication(
            higher,
            lower
        ))

        self.assertIs(self.socket_layer._higher, higher)
        self.assertIs(self.socket_layer._lower, lower)

if __name__ == '__main__':
    unittest.main()