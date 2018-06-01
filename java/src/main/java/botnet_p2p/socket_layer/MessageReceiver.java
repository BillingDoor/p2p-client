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

    public MessageReceiver(BlockingQueue<ByteBuffer> receivedMessages) {
        this.receivedMessages = receivedMessages;
    }


    void handleNewMessage(SelectableChannel channel) throws IOException {
        SocketChannel client = (SocketChannel) channel;
        ByteBuffer inputBuffer = ByteBuffer.allocate(512);
        try {
            if (client.read(inputBuffer) == -1) {
                client.close();
                return;
            }
        } catch (IOException e) {
            if (e.getMessage().equals("An existing connection was forcibly closed by the remote host")) {
                logger.info("client has disconnected in a dirty way " + client.getLocalAddress());
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
