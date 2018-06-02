from python.Socket.SocketLayer import SocketLayer
import python.Protobuf.protobuf_utils as putils
from python.Message.MessageLayer import MessageLayer
from python.Business.BusinessLogicLayer import BusinessLogicLayer
from python.P2P.BucketList import BucketList
from python.P2P.peer import Peer
from python.StatusMessage import StatusMessage
import random
import logging.handlers
import asyncio

logging.basicConfig(
    level=logging.DEBUG,
    format='%(name)s: %(message)s',
)
handler = logging.handlers.RotatingFileHandler(
    "log.txt",
    maxBytes=65536,
    backupCount=10
)
log = logging.getLogger(__name__)
log.addHandler(handler)
formatter = logging.Formatter('%(name)s: %(message)s')
handler.formatter = formatter

class P2PLayer:
    def __init__(self, lower_layer, address, port, id = None):
        self.lower_layer = lower_layer
        self.log = logging.getLogger(__name__)

        self._this_peer = Peer(id, address, port)
        self._routing_table = BucketList(bucket_size=5, buckets_number=64, id=self._this_peer.id)

    async def add_layer_communication(self, higher, lower):
        """
        Adds means of communicating with lower and/or lower layer. Higher and lower should be a tuple of two objects
        that support asynchronous communication using get() and put() method to pass along data.
        Then starts listening on them for data.
        :param higher: Tuple of two objects for communication with higher layer
        :param lower: Tuple of two objects for communication with lower layer
        """
        self._higher = higher
        self._lower = lower
        asyncio.ensure_future(self._handle_lower_input())
        asyncio.ensure_future(self._handle_higher_input())

    async def _handle_lower_input(self):
        try:
            while True:
                log.debug("Waiting for message from lower layer")
                message = await self._lower[0].get()
                log.debug("Got message {!r}, sending it to higher layer".format(message))
                await self._higher[1].put(message)
                log.debug("Message {!r} sent to the higher layer".format(message))
        except asyncio.CancelledError:
            log.debug("Caught CancelledError: Stop handling input from lower layer")

    async def _handle_higher_input(self):
        try:
            while True:
                log.debug("Waiting for message from higher layer")
                message = await self._higher[0].get()
                log.debug("Got message {!r}, sending it to lower layer".format(message))
                await self._lower[1].put(message)
                log.debug("Message {!r} sent to the lower layer".format(message))
        except asyncio.CancelledError:
            log.debug("Caught CancelledError: Stop handling input from higher layer")


    def get_myself(self):
        """
        Returns this_peer
        :return: Peer object of this node
        """
        return self._this_peer

    async def get_peer_by_id(self, id):
        """
        Returns peer with wanted id
        :param id: id of wanted peer
        :return: Peer object with wanted id
        """
        return await self._routing_table.get_peer_by_id(id)


    async def ping(self, target_id):
        """
        Sends ping message to peer with given target_id
        :param target_id: id of target peer
        :return: SUCCESS or ERROR
        """
        peer = await self._routing_table.get_peer_by_id(target_id)
        if peer is None:
            return StatusMessage.FAILURE
        message = putils.create_ping_message(sender=self._this_peer, receiver=peer)

        self.log.debug("Putting message to Queue {!r}".format(self._lower[1]))
        try:
            await self._lower[1].put(message)
            self.log.debug("Message {} put into Queue {}".format(message, self._lower[1]))
            return StatusMessage.SUCCESS
        except asyncio.CancelledError:
            self.log.debug("Message {} has not been put onto {} because CancelledError was caught".format(
                message,
                self._lower[1]
            ))
            return StatusMessage.ERROR
