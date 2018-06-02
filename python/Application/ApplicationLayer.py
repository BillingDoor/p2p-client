from python.Socket.SocketLayer import SocketLayer
from python.Protobuf.protobuf_utils import *
from python.P2P.P2PLayer import P2PLayer
from python.Message.MessageLayer import MessageLayer
from python.Business.BusinessLogicLayer import BusinessLogicLayer
import asyncio
import logging

logging.basicConfig()

class Application:
    def __init__(self, business_logic_layer):
        self.business_logic_layer = business_logic_layer
        self.log = logging.getLogger('Application')

    async def add_layer_communication(self, higher=None, lower=None):
        """
        Adds means of communicating with lower and/or lower layer. Higher and lower should be a tuple of two objects
        that support asynchronous communication using get() and put() method to pass along data.
        :param higher: Tuple of two objects for communication with higher layer
        :param lower: Tuple of two objects for communication with lower layer
        """
        if higher:
            self.higher = higher

        if lower:
            self.lower = higher
