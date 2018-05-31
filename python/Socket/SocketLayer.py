import socketserver
from rx import Observer, Observable

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
        print("UID:{}  TARGET_GUID:{}".format(id, target_id))
        self.server.node.routing_table.insert(Peer(address, port, id))

    def _handle_leave(self, message):
        """
        Handles leave message
        """

    def _handle_default(self, message):
        """
        Handles other messages
        """

class SocketLayer:
    def __init__(self):
        self.server = Server(self.peer.address(), RequestHandler, self)
