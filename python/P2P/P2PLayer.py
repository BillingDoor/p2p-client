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

# class KademliaNode(object):
#     def __init__(self, host, port, id=None, seeds=[], requesthandler=RequestHandler):
#         # Create kademlia node info object
#         self.peer = Peer(host, port, id)
#         self.other_peers = []
#         # Create Server
#         self.server = Server(self.peer.address(), requesthandler, self)
#         # Lock so server and client won't send data at the same time
#         self.send_lock = threading.Lock
#
#         self.server_thread = threading.Thread(target=self.server.serve_forever)
#         self.server_thread.daemon = True
#         self.server_thread.start()
#
#         self.routing_table = BucketList(5, 64, self.peer.id)
#         self.bootstrap(seeds)
#
#     def find_nodes(self, key, boot_peer=None):
#         """
#         Send find_node message with id of this node to bootstrap node and every other node that is returned
#         """
#         # First we send FIND_NODE message to bootstrap node and in response get nodes that are closer to us than
#         # bootstrap node. Then we insert them in our routing table send FIND_MESSAGE to them. We repeat that iteratively
#         # and in the end we populate our routing table with all nodes between us and the booting node.
#         nodes_to_ask = []
#         asked_nodes = [boot_peer]
#         found_nodes = boot_peer.find_node(key, self.peer.host, self.peer.port)
#         nodes_to_ask.extend(found_nodes.pFoundNodes.nodes)
#
#
#         for node in nodes_to_ask:
#             guid = node.guid
#             ip = node.IP
#             port = int(node.Port)
#             is_NAT = node.isNAT
#             new_peer = Peer(ip, port, guid, is_NAT)
#             self.routing_table.insert(new_peer)
#
#             found_nodes = new_peer.find_node(key, self.peer.host, self.peer.port)
#             asked_nodes.append(new_peer)
#             for node in found_nodes.pFoundNodes.nodes:
#                 if Peer(node.IP, int(node.Port), node.guid, node.isNAT) not in asked_nodes:
#                     nodes_to_ask.append(found_nodes.pFoundNodes.nodes)
#
#         # After that we need to populate buckets that are further than bootstrap node by doing lookup of random
#         # key that falls into that bucket
#
#         bootstrap_bucket_index = largest_differing_bit(key, boot_peer.id)
#
#         for i in range(bootstrap_bucket_index + 1, len(self.routing_table.buckets)):
#             id = key
#             # Change bit on i position (from the most significant bit, indexing from 0)
#             mask = 1 << (len(self.routing_table.buckets) - i - 1)
#             id ^= mask
#
#             _ = self.lookup_node(id)
#
#     def bootstrap(self, bootstrap_nodes = []):
#         for bnode in bootstrap_nodes:
#             boot_peer = Peer(bnode[0], bnode[1], bnode[2])
#             self.routing_table.insert(boot_peer)
#             self.find_nodes(self.peer.id, boot_peer=boot_peer)
#
#     def lookup_node(self, id, alfa=3):
#         """
#         Search for peer with given id. First we check if our routing table does not hold it already and if not we
#         ask other peers for information about it.
#         :param id: id of our wanted peer.
#         :return: Peer or None if could not find.
#         """
#         peer = self.routing_table[id]
#         if peer:
#             return peer
#
#         k = self.routing_table.bucket_size
#         peers_to_ask = self.routing_table.nearest_nodes(id, limit=k)
#
#         smallest_distance = peers_to_ask[0].id ^ id
#
#         while True:
#             found_peers = []
#             for peer in peers_to_ask:
#                 for node in peer.find_node(id, self.peer.host, self.peer.port).pFoundNodes.nodes:
#                     guid = node.guid
#                     ip = node.IP
#                     port = int(node.Port)
#                     is_NAT = node.isNAT
#                     new_peer = Peer(ip, port, guid, is_NAT)
#                     found_peers.append(new_peer)
#
#             # If we didn't receive any new peers
#             if not found_peers:
#                 break
#
#             # Now we get k best peers and insert them into our routing table
#             best_peers = heapq.nsmallest(k, found_peers, lambda p: p.id ^ id)
#             for peer in best_peers:
#                 self.routing_table.insert(peer)
#             # If we didn't get any peer closer to our wanted peer break (we should now have all necessary nodes
#             if best_peers[0].id  ^ id <= smallest_distance:
#                 break
#             # And ask them about our wanted peer
#             peers_to_ask = best_peers
#
#         # After asking other peers we ask nearest k nodes about our node by sending FIND_VALUE message
#         peers_to_ask = self.routing_table.nearest_nodes(id, limit=k)
#
#         for peer in peers_to_ask:
#             found_peer = peer.find_value(id, self.peer.host, self.peer.port)
#             if found_peer:
#                 self.routing_table.insert(found_peer)
#                 return found_peer



class P2PLayer:
    def __init__(self, lower_layer, address, port, id = None):
        self.lower_layer = lower_layer
        self.log = logging.getLogger(__name__)

        #create peer containing info about this node
        local_random = random.Random()
        local_random.seed(int(''.join(address.split('.'))) * int(port))
        if id is None:
            id = local_random.getrandbits(64)
        self._this_peer = Peer(id, address, port)

        self._routing_table = BucketList(5, 64, self._this_peer.id)

    async def add_layer_communication(self, higher, lower):
        """
        Adds means of communicating with lower and/or lower layer. Higher and lower should be a tuple of two objects
        that support asynchronous communication using get() and put() method to pass along data.
        Then starts listening on queues for data.
        :param higher: Tuple of two objects for communication with higher layer
        :param lower: Tuple of two objects for communication with lower layer
        """
        self._higher = higher
        self._lower = lower
        asyncio.ensure_future(self._handle_lower_in())
        asyncio.ensure_future(self._handle_higher_in())

    async def _handle_lower_in(self):
        try:
            while True:
                log.debug("P2P: Waiting for message from lower layer")
                message = await self._lower[0].get()
                log.debug("P2P: Got message {!r}, sending it to higher layer".format(message))
                await self._higher[1].put(message)
                log.debug("P2P: Message {!r} sent to the higher layer".format(message))
        except asyncio.CancelledError:
            log.debug("P2P: Caught CancelledError: Stop handling input from lower layer")

    async def _handle_higher_in(self):
        try:
            while True:
                log.debug("P2P: Waiting for message from higher layer")
                message = await self._higher[0].get()
                log.debug("P2P: Got message {!r}, sending it to lower layer".format(message))
                await self._lower[1].put(message)
                log.debug("P2P: Message {!r} sent to the lower layer".format(message))
        except asyncio.CancelledError:
            log.debug("P2P: Caught CancelledError: Stop handling input from higher layer")


    def get_myself(self):
        """
        Returns this_peer
        :return: Peer object of this node
        """
        return self._this_peer

    async def get_peer(self, id):
        """
        Returns peer with wanted id
        :param id: id of wanted peer
        :return: Peer object with wanted id
        """
        return await self._routing_table.get(id)


    async def ping(self, target_id):
        """
        Sends ping message to peer with given target_id
        :param target_id: id of target peer
        :return: SUCCESS or ERROR
        """
        peer = await self._routing_table.get(target_id)
        if peer is None:
            return StatusMessage.FAILURE
        message = putils.create_ping_message(self._this_peer, peer)

        self.log.debug("Putting message to Queue {!r}".format(self._lower[1]))
        try:
            await self._lower[1].put(message)
            self.log.debug("P2P: Message {} put into Queue {}".format(message, self._lower[1]))
            return StatusMessage.SUCCESS
        except asyncio.CancelledError:
            self.log.debug("P2P: Message {} has not been put onto {} because CancelledError was caught".format(
                message,
                self._lower[1]
            ))
            return StatusMessage.ERROR
