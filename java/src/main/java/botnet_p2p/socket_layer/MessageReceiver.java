package botnet_p2p.socket_layer;

import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

class MessageReceiver {
    private static final Logger logger = LogManager.getLogger(MessageReceiver.class);
    private BlockingQueue<ByteBuffer> receivedMessages;
    private NodeManager nodeManager;
    private Map<SocketAddress, PartialMessage> partialMessages = new HashMap<>();

    @AllArgsConstructor
    class PartialMessage {
        public ByteBuffer data;
        public int msgSize;
        public int storedBytes;
    }

    MessageReceiver(BlockingQueue<ByteBuffer> receivedMessages, NodeManager nodeManager) {
        this.receivedMessages = receivedMessages;
        this.nodeManager = nodeManager;
    }


    void handleNewMessage(SelectableChannel channel) throws IOException {
        SocketChannel client = (SocketChannel) channel;
        ByteBuffer inputBuffer = ByteBuffer.allocate(12288);
        int bytesReceived = 0;
        try {
            if ((bytesReceived = client.read(inputBuffer)) == -1) {
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

        // TODO getRemoteAddress ?

        inputBuffer.rewind();
        int bytesReadFromInput = 0;
        while (bytesReadFromInput < bytesReceived) {
            if (partialMessages.containsKey(client.getRemoteAddress())) {
                PartialMessage partialMessage = partialMessages.get(client.getRemoteAddress());

                if (bytesReceived + partialMessage.storedBytes >= partialMessage.msgSize) {
                    // parse message from partialMessage - full message read
                    partialMessage.data.put(inputBuffer.array(), 0, partialMessage.msgSize - partialMessage.storedBytes);
                    bytesReadFromInput += (partialMessage.msgSize - partialMessage.storedBytes);

                    partialMessage.data.position(0);
                    this.receivedMessages.offer(partialMessage.data);
                    partialMessage.data.clear();
                    this.partialMessages.remove(client.getRemoteAddress());
                } else {
                    // not enough, just add
                    logger.debug("still not whole message");
                    partialMessage.data.put(inputBuffer);
                    return;
                }
            }

            if(bytesReadFromInput >= bytesReceived) {
                return;
            }

            int msgSize = inputBuffer.getInt(bytesReadFromInput);
            bytesReadFromInput += Integer.BYTES;
            if (bytesReceived - bytesReadFromInput >= msgSize) { // whole message has been read
                ByteBuffer messageBuffer = ByteBuffer.wrap(inputBuffer.array(), bytesReadFromInput, msgSize);
                bytesReadFromInput += msgSize;
                if (!this.receivedMessages.offer(messageBuffer)) {
                    logger.error("queue is full!");
                }
            } else {
                ByteBuffer remainingData = ByteBuffer.allocate(12288);
                remainingData.put(inputBuffer.array(), bytesReadFromInput-4, bytesReceived);
                // remainingData.position(bytesReceived - bytesReadFromInput);
                partialMessages.put(client.getRemoteAddress(),
                        new PartialMessage(remainingData, msgSize, bytesReceived - bytesReadFromInput));
                logger.debug("did not read whole message");
                // TODO handle partial message
                return;
            }
        }

        inputBuffer.clear();
    }


    private void readMessageFromFile() {

    }
}
