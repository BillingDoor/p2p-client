from python.peer import Peer
from python.BucketList import BucketList, largest_differing_bit
import python.protobuf_utils as putils
import socket
import socketserver
import threading
import heapq

class Server(socketserver.ThreadingMixIn, socketserver.TCPServer):
    def __init__(self, server_address, request_handler_class, kademlia_node):
        socketserver.TCPServer.__init__(self, server_address, request_handler_class)
        self.node = kademlia_node

class RequestHandler(socketserver.BaseRequestHandler):
    def handle(self):
        # self.request is the TCP socket connected to the client
        data = self.request.recv(12000)
        message = putils.read_message(data)
        """
        UNDEFINED = 0;
        COMMAND = 1;
        RESPONSE = 2;
        FILE_CHUNK = 3;
        NAT_REQUEST = 4;
        NAT_CHECK = 5;
        PING = 6;
        LEAVE = 7;
        FIND_NODE = 8;
        FOUND_NODES = 9;
        FIND_VALUE = 10;
        """
        if message.type == message.PING:
            self._handle_ping(message)
        elif message.type == message.FIND_NODE:
            self._handle_find_node(message)
        elif message.type == message.FOUND_NODES:
            self._handle_found_nodes(message)
        elif message.type == message.FIND_VALUE:
            self._handle_find_value(message)
        elif message.type == message.LEAVE:
            self._handle_leave(message)
        else:
            self._handle_default(message)

    def _handle_ping(self, message):
        """
        Handles ping message
        """
        address, port = message.sender.split(':')
        port = int(port)
        id = message.uuid
        self.server.node.routing_table.insert(Peer(address, port, id))

        # Send response
        msg = putils.create_ping_message(self.server.node.peer.id,
                                         self.server.node.peer.host,
                                         self.server.node.peer.port)
        self.request.send(msg)

    def _handle_find_node(self, message):
        """
        Handles find node message
        """
        target_id = message.pFindNode.guid
        closest_peers = self.server.node.routing_table.nearest_nodes(target_id, limit=self.server.node.routing_table.bucket_size)

        msg = putils.create_found_nodes_message(self.server.node.peer.id, closest_peers,
                                                self.server.node.peer.host, self.server.node.peer.port)
        self.request.send(msg)

        address, port = message.sender.split(':')
        port = int(port)
        id = message.uuid
        self.server.node.routing_table.insert(Peer(address, port, id))

    def _handle_found_nodes(self, message):
        """
        Handles found nodes message
        """

    def _handle_find_value(self, message):
        """
        Handles find value message
        """
        target_id = message.pFindNode.guid
        peer = self.server.node.routing_table[target_id]
        if peer:
            peers = [peer]
        else:
            peers = []
        msg = putils.create_found_nodes_message(self.server.node.peer.id, peers,
                                                self.server.node.peer.host, self.server.node.peer.port)
        self.request.send(msg)

        address, port = message.sender.split(':')
        port = int(port)
        id = message.uuid
        self.server.node.routing_table.insert(Peer(address, port, id))

    def _handle_leave(self, message):
        """
        Handles leave message
        """

    def _handle_default(self, message):
        """
        Handles other messages
        """


class KademliaNode(object):
    def __init__(self, host, port, id=None, seeds=[], requesthandler=RequestHandler):
        # Create kademlia node info object
        self.peer = Peer(host, port, id)
        self.other_peers = []
        # Create Server
        self.server = Server(self.peer.address(), requesthandler, self)
        # Lock so server and client won't send data at the same time
        self.send_lock = threading.Lock

        self.server_thread = threading.Thread(target=self.server.serve_forever)
        self.server_thread.daemon = True
        self.server_thread.start()

        self.routing_table = BucketList(5, 64, self.peer.id)
        self.bootstrap(seeds)

    def find_nodes(self, key, boot_peer=None):
        """
        Send find_node message with id of this node to bootstrap node and every other node that is returned
        """
        # First we send FIND_NODE message to bootstrap node and in response get nodes that are closer to us than
        # bootstrap node. Then we insert them in our routing table send FIND_MESSAGE to them. We repeat that iteratively
        # and in the end we populate our routing table with all nodes between us and the booting node.
        nodes_to_ask = []
        asked_nodes = [boot_peer]
        found_nodes = boot_peer.find_node(key, self.peer.host, self.peer.port)
        nodes_to_ask.extend(found_nodes.pFoundNodes.nodes)


        for node in nodes_to_ask:
            guid = node.guid
            ip = node.IP
            port = int(node.Port)
            is_NAT = node.isNAT
            new_peer = Peer(ip, port, guid, is_NAT)
            self.routing_table.insert(new_peer)

            found_nodes = new_peer.find_node(key, self.peer.host, self.peer.port)
            asked_nodes.append(new_peer)
            for node in found_nodes.pFoundNodes.nodes:
                if Peer(node.IP, int(node.Port), node.guid, node.isNAT) not in asked_nodes:
                    nodes_to_ask.append(found_nodes.pFoundNodes.nodes)

        # After that we need to populate buckets that are further than bootstrap node by doing lookup of random
        # key that falls into that bucket

        bootstrap_bucket_index = largest_differing_bit(key, boot_peer.id)

        for i in range(bootstrap_bucket_index + 1, len(self.routing_table.buckets)):
            id = key
            # Change bit on i position (from the most significant bit, indexing from 0)
            mask = 1 << (len(self.routing_table.buckets) - i - 1)
            id ^= mask

            _ = self.lookup_node(id)

    def bootstrap(self, bootstrap_nodes = []):
        for bnode in bootstrap_nodes:
            boot_peer = Peer(bnode[0], bnode[1], bnode[2])
            self.routing_table.insert(boot_peer)
            self.find_nodes(self.peer.id, boot_peer=boot_peer)

    def lookup_node(self, id, alfa=3):
        """
        Search for peer with given id. First we check if our routing table does not hold it already and if not we
        ask other peers for information about it.
        :param id: id of our wanted peer.
        :return: Peer or None if could not find.
        """
        peer = self.routing_table[id]
        if peer:
            return peer

        k = self.routing_table.bucket_size
        peers_to_ask = self.routing_table.nearest_nodes(id, limit=k)

        smallest_distance = peers_to_ask[0].id ^ id

        while True:
            found_peers = []
            for peer in peers_to_ask:
                for node in peer.find_node(id, self.peer.host, self.peer.port).pFoundNodes.nodes:
                    guid = node.guid
                    ip = node.IP
                    port = int(node.Port)
                    is_NAT = node.isNAT
                    new_peer = Peer(ip, port, guid, is_NAT)
                    found_peers.append(new_peer)

            # If we didn't receive any new peers
            if not found_peers:
                break

            # Now we get k best peers and insert them into our routing table
            best_peers = heapq.nsmallest(k, found_peers, lambda p: p.id ^ id)
            for peer in best_peers:
                self.routing_table.insert(peer)
            # If we didn't get any peer closer to our wanted peer break (we should now have all necessary nodes
            if best_peers[0].id  ^ id <= smallest_distance:
                break
            # And ask them about our wanted peer
            peers_to_ask = best_peers

        # After asking other peers we ask nearest k nodes about our node by sending FIND_VALUE message
        peers_to_ask = self.routing_table.nearest_nodes(id, limit=k)

        for peer in peers_to_ask:
            found_peer = peer.find_value(id, self.peer.host, self.peer.port)
            if found_peer:
                self.routing_table.insert(found_peer)
                return found_peer

