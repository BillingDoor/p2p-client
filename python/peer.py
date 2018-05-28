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

    def find_node(self, ID):
        """
        Send FIND_NODE message containing given ID to this peer
        """
        # Make socket and connect to this
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect(self.address())

        msg = putils.create_find_node_message(self.id, ID, self.host, self.port)
        sock.send(msg)

        # Code below will be changed later to accommodate server-client architecture
        response = sock.recv(12000)
        sock.close()

        found_nodes_message = putils.read_message(response)
        return found_nodes_message

    def find_value(self, ID):
        """
        Send FIND_VALUE message containing given ID to this peer
        :param ID: id of wanted peer
        :return: Found Peer or None
        """
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect(self.address())

        msg = putils.create_find_value_message(self.id, ID, self.host, self.port)
        sock.send(msg)

        response = sock.recv(12000)
        sock.close()

        found_node = putils.read_message(response)
        return

    def get_info(self):
        return self.host, self.port, self.id

    def ping(self, socket=None):
        data = "Hello"
        self._sendmessage(data, socket)
        self._receivemessage(socket)

    def _sendmessage(self, message, sock=None):
        mess = python.Message_pb2.Message()
        mess.TYPE = mess.JOIN
        if sock:
            sock.sendall(mess.SerializeToString())

    def _receivemessage(self, socket):
        mess = socket.recv(12000)
        message = python.Message_pb2.Message()
        message.ParseFromString(mess)

        print(mess)
