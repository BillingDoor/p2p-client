import unittest.mock
import asyncio
from python.Socket.SocketLayer import SocketLayer
from python.Protobuf.protobuf_utils import *
from python.P2P.P2PLayer import P2PLayer
from python.Message.MessageLayer import MessageLayer
from python.Business.BusinessLogicLayer import BusinessLogicLayer
from python.StatusMessage import StatusMessage

def _run(cor):
    return asyncio.get_event_loop().run_until_complete(cor)

class BusinessLayerTest(unittest.TestCase):
    def setUp(self):
        sl = SocketLayer()
        ml = MessageLayer(sl)
        p2pl = P2PLayer(ml, "127.0.0.1", 8080)
        self.business_layer = BusinessLogicLayer(p2pl)

    def test_setting_up_ilc(self):
        q1 = asyncio.Queue()
        q2 = asyncio.Queue()
        q3 = asyncio.Queue()
        q4 = asyncio.Queue()
        higher = (q1, q2)
        lower = (q3, q4)

        _run(self.business_layer.add_layer_communication(
            higher,
            lower
        ))

        self.assertIs(self.business_layer._higher, higher)
        self.assertIs(self.business_layer._lower, lower)

    @unittest.mock.patch(python.P2P.P2PLayer.join_network)
    def test_join_network(self):
        """
        BLL calls join_network on message layer,
        and on success calls callback if any and after that returns success
        """
        status = self.business_layer.join_network(("127.0.0.1", 8080))
        self.assertIs(status, StatusMessage.SUCCESS)


if __name__ == '__main__':
    unittest.main()