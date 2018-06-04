from python.Socket.SocketLayer import SocketLayer
from python.Protobuf.protobuf_utils import *
from python.P2P.P2PLayer import P2PLayer
from python.Message.MessageLayer import MessageLayer
from python.Business.BusinessLogicLayer import BusinessLogicLayer
import asyncio
import logging.handlers
import os

logging.basicConfig(
    level=logging.DEBUG,
    format='%(name)s: %(message)s',
)
handler = logging.handlers.RotatingFileHandler(
    os.path.abspath("./logs/log.txt"),
    maxBytes=65536,
    backupCount=10
)

log = logging.getLogger(__name__)

class Application:
    def __init__(self, business_logic_layer):
        self.business_logic_layer = business_logic_layer

    async def run(self):
        """
        Run application
        :return:
        """
