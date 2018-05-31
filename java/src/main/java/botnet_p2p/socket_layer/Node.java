package botnet_p2p.socket_layer;


import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

class Node {
    SocketChannel socketChannel;
    InetSocketAddress address;
    NodeStatus status;

    Node(InetSocketAddress address, NodeStatus status) {
        this.address = address;
        this.status = status;
    }

    Node(SocketChannel socketChannel, InetSocketAddress address, NodeStatus status) {
        this.socketChannel = socketChannel;
        this.address = address;
        this.status = status;
    }
}
