from python.P2P.BucketList import BucketList
from python.P2P.peer import Peer
import logging.handlers
import asyncio
import os

logging.basicConfig(
    level=logging.DEBUG,
    format='%(name)s: %(message)s',
)
handler = logging.handlers.RotatingFileHandler(
    os.path.abspath("./logs/log.txt"),
)
log = logging.getLogger(__name__)
log.addHandler(handler)
formatter = logging.Formatter('%(name)s: %(message)s')
handler.formatter = formatter
log.propagate = False

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
            return

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
            return

    async def get_routing_table_info(self):
        routing_table_info = await self._routing_table.get_routing_table_info()
        return routing_table_info

    def start_server(self):
        self.lower_layer.start_server(self.get_myself().ip, self.get_myself().port)

    async def stop_server(self):
        await self.lower_layer.stop_server()

    async def add_peer(self, peer):
        """
        Adds new peer to the routing table
        :param peer: Peer to add
        """
        await self._routing_table.insert(peer)

    async def remove_peer(self, peer):
        """
        Remove peer from the routing table
        :param peer: Peer to remove
        """
        await self._routing_table.remove(peer)

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

    async def get_nearest_peers(self, wanted_peer_id, limit=None):
        """
        Return peers nearest to the one with wanted id
        :param wanted_peer_id: Id of wanted peer
        :return: List of nearest Peers
        """
        return await self._routing_table.nearest_nodes(wanted_peer_id, limit=limit)
