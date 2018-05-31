package botnet.socket_layer;


import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

class PendingMessage {
    InetSocketAddress destination;
    ByteBuffer message;

    PendingMessage(InetSocketAddress destination, ByteBuffer message) {
        this.destination = destination;
        this.message = message;
    }
}