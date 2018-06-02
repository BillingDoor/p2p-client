package botnet_p2p.message_layer;

import botnet_p2p.model.Communication;
import botnet_p2p.socket_layer.SocketLayer;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;

import static botnet_p2p.MessageOuterClass.Message;


public class MessageLayer extends Thread {
    private static final Logger logger = LogManager.getLogger(MessageLayer.class);

    private SocketLayer socketLayer;
    private BlockingQueue<ByteBuffer> receivedMessages; // socket layer will put here received messages
    private BlockingQueue<Message> decodedMessages;

    public MessageLayer(SocketLayer socketLayer,
                        BlockingQueue<ByteBuffer> receivedMessages,
                        BlockingQueue<Message> decodedMessages
    ) {
        this.socketLayer = socketLayer;

        this.receivedMessages = receivedMessages;
        this.decodedMessages = decodedMessages;
    }


    public void send(Communication<Message> message) {
        try {
            socketLayer.send(new Communication<>(
                    message.getData().toByteString().asReadOnlyByteBuffer(),
                    message.getPeer()
            ));
        } catch (IOException e) {
            logger.error("unable to send message");
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                ByteBuffer bytes = receivedMessages.take();
                decodedMessages.offer(Message.parseFrom(bytes));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (InvalidProtocolBufferException e) {
                logger.info("invalid message, ignoring");
            }
        }
        logger.info("closing - loop ended");
    }

    public void shutdown() {
        logger.info("closing");
        this.interrupt();
        this.socketLayer.shutdown();
    }
}
