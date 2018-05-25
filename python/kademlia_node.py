from python.peer import Peer
from python.BucketList import BucketList, largest_differing_bit
import python.Message_pb2
import socket
import socketserver
import threading
import heapq

class Server(socketserver.ThreadingMixIn, socketserver.TCPServer):
    def __init__(self, server_address, request_handler_class):
        socketserver.TCPServer.__init__(self, server_address, request_handler_class)
        self.kademlia_node = None

class RequestHandler(socketserver.BaseRequestHandler):
    def handle(self):
        # self.request is the TCP socket connected to the client
        self.data = self.request.recv(12000).strip()
        message = python.Message_pb2.Message().ParseFromString(self.data)
        print("{} wrote: ".format(self.client_address[0]))
        print(message)



class KademliaNode(object):
    def __init__(self, host, port, id=None, seeds=[], requesthandler=RequestHandler):
        # Create kademlia node info object
        self.peer = Peer(host, port)
        self.other_peers = []
        # Create Server
        self.server = Server(self.peer.address(), requesthandler)
        self.server.kademlia_node = self

        self.server_thread = threading.Thread(target=self.server.serve_forever)
        self.server_thread.daemon = True
        self.server_thread.start()

        self.routing_table = BucketList(5, 64, self.peer.id)
        self.peers_connections = []
        self.bootstrap(seeds)

    def find_nodes(self, key, boot_peer=None):
        """
        Send find_node message with id of this node to bootstrap node and every other node that is returned
        """
        # First we send FIND_NODE message to bootstrap node and in response get nodes that are closer to us than
        # bootstrap node. Then we insert them in our routing table send FIND_MESSAGE to them. We repeat that iteratively
        # and in the end we populate our routing table with all nodes between us and the booting node.
        nodes_to_ask = []
        found_nodes = boot_peer.find_node(key)
        nodes_to_ask.extend(found_nodes.pFoundNodes.nodes)

        for node in nodes_to_ask:
            guid = node.guid
            ip = node.IP
            port = node.Port
            new_peer = Peer(ip, port, guid)
            self.routing_table.insert(new_peer)

            found_nodes = new_peer.find_node(key)
            nodes_to_ask.extend(found_nodes.pFoundNodes.nodes)

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
            boot_peer = Peer(bnode[0], bnode[1])
            self.find_nodes(self.peer.id, boot_peer=boot_peer)

        if len(bootstrap_nodes) == 0:
            for bnode in self.buckets.to_list():
                self.find_nodes(self.peer.id, boot_peer=Peer(bnode[0], bnode[1], bnode[2], bnode[3]))

        for bnode in bootstrap_nodes:
            print("Bootstraping")
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.peers_connections.append(sock)

            boot_peer = Peer(bnode[0], bnode[1])
            self.other_peers.append(boot_peer)

            sock.connect(boot_peer.address())
            boot_peer.ping(socket=sock)

    def lookup_node(self, id, alfa=3):
        """
        Search for peer with given id. First we check if our routing table does not hold it already and if not we
        ask other peers for information about it.
        :param id: id of our wanted peer.
        :return: Peer or None if could not find.
        """

        k = self.routing_table.bucket_size
        nearest_nodes = self.routing_table.nearest_nodes(id, limit=k)

        found_nodes = []
        for peer in nearest_nodes:
            found_nodes.extend(peer.find_node(id).pFoundNodes.nodes)

        found_peers = []
        for node in found_nodes:
            guid = node.guid
            ip = node.IP
            port = node.Port
            new_peer = Peer(ip, port, guid)
            found_peers.append(new_peer)

        # Now we get k best peers
        best_peers = heapq.nsmallest(k, found_peers, lambda peer: peer.)

        # get only k best results
        heapq.nsmallest(k, found_nodes, lambda p: key ^ p.get_info()[2])

        nodes_to_ask = []
        found_nodes = boot_peer.find_node(key)
        nodes_to_ask.extend(found_nodes.pFoundNodes.nodes)

        for peer in nodes_to_ask:
            guid = peer.guid
            ip = peer.IP
            port = peer.Port
            new_peer = Peer(ip, port, guid)
            self.routing_table.insert(new_peer)

            found_nodes = new_peer.find_node(key)
            nodes_to_ask.extend(found_nodes.pFoundNodes.nodes)
