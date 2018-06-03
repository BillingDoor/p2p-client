package botnet_p2p.socket_layer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;

class MessageReceiver {
    private static final Logger logger = LogManager.getLogger(MessageReceiver.class);
    private BlockingQueue<ByteBuffer> receivedMessages;
    private NodeManager nodeManager;

    MessageReceiver(BlockingQueue<ByteBuffer> receivedMessages, NodeManager nodeManager) {
        this.receivedMessages = receivedMessages;
        this.nodeManager = nodeManager;
    }


    void handleNewMessage(SelectableChannel channel) throws IOException {
        SocketChannel client = (SocketChannel) channel;
        ByteBuffer inputBuffer = ByteBuffer.allocate(12288);
        try {
            if (client.read(inputBuffer) == -1) {
                nodeManager.removeNode(client.getRemoteAddress());
                client.close();
                return;
            }
        } catch (IOException e) {
            if (e.getMessage().equals("An existing connection was forcibly closed by the remote host")) {
                logger.info("client has disconnected in a dirty way " + client.getLocalAddress());
                nodeManager.removeNode(client.getRemoteAddress());
                client.close();
                return;
            } else {
                throw e;
            }
        }

        ByteBuffer messageBuffer = ByteBuffer.wrap(inputBuffer.array(), 0, inputBuffer.position());
        if (!this.receivedMessages.offer(messageBuffer)) {
            logger.error("queue is full!");
        }
        inputBuffer.clear();
    }
}
