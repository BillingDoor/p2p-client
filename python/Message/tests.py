import unittest
from python.Socket.SocketLayer import SocketLayer
from python.Protobuf.protobuf_utils import *
from python.P2P.P2PLayer import P2PLayer
from python.Message.MessageLayer import MessageLayer
from python.Business.BusinessLogicLayer import BusinessLogicLayer
from python.StatusMessage import StatusMessage

class SocketsTests(unittest.TestCase):
    def setUp(self):
        sl = SocketLayer()
        self.ml =  MessageLayer(sl)

    def ping(self):
        """
        MessageLayer calls ping on socket layer and it should receive
        """
        status = self.bl.join_network(("127.0.0.1", 8080))
        self.assertIs(status, StatusMessage.SUCCESS)


