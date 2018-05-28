import python.Message_pb2
import random
import socket
import python.protobuf_utils as putils
class Peer(object):
    """
    Peer
    """

    def __init__(self, host, port, guid, is_NAT = False):
        self.host, self.port = host, port
        local_random = random.Random()
        local_random.seed(int(''.join(host.split('.')))*int(port))
        if guid is None:
            self.id = local_random.getrandbits(64)
        else:
            self.id = guid
        self.is_NAT = is_NAT

    def __eq__(self, other):
        return self.get_info() == other.get_info()

    def address(self):
        return (self.host, self.port)

    def find_node(self, ID, address, port):
        """
        Send FIND_NODE message containing given ID to this peer
        :param ID: id of peer we want to find
        :param address: IP address of source peer
        :param port: port of source peer

        """
        # Make socket and connect to this
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect(self.address())

        msg = putils.create_find_node_message(ID, ID, address, port)
        sock.send(msg)

        # Code below will be changed later to accommodate server-client architecture
        response = sock.recv(12000)
        sock.close()

        found_nodes_message = putils.read_message(response)
        return found_nodes_message

    def find_value(self, ID, address, port):
        """
        Send FIND_VALUE message containing given ID to this peer
        :param ID: id of wanted peer
        :param address: IP address of source peer
        :param port: port of source peer
        :return: Found Peer or None
        """
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect(self.address())

        msg = putils.create_find_value_message(ID, ID, address, port)
        sock.send(msg)

        response = sock.recv(12000)
        sock.close()

        found_node_message = putils.read_message(response)
        node = found_node_message.pFoundNodes.nodes
        if node:
            guid = node.guid
            ip = node.IP
            port = int(node.Port)
            is_NAT = node.isNAT
            new_peer = Peer(ip, port, guid, is_NAT)
            return new_peer
        return None


    def get_info(self):
        return self.host, self.port, self.id

    def ping(self, address, port):
        """
        Send PING message to this peer
        :param address: IP address of source peer
        :param port: port of source peer
        :return:
        """
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect(self.address())

        msg = putils.create_ping_message(self.id, address, port)
        sock.send(msg)

        response = sock.recv(12000)
        sock.close()

        ping_response = putils.read_message(response)
        return ping_response

