package botnet_p2p.message_layer;

import botnet_p2p.model.Communication;
import botnet_p2p.socket_layer.SocketLayer;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static botnet_p2p.MessageOuterClass.Message;


public class MessageLayer extends Thread {
    private static final Logger logger = LogManager.getLogger(MessageLayer.class);

    private SocketLayer socketLayer;
    private BlockingQueue<ByteBuffer> receivedMessages; // socket layer will put here received messages
    private BlockingQueue<Message> decodedMessages;
    // private BlockingQueue<Communication<ByteBuffer>> outgoingMessages; // socket layer will take messages to send from here


    public MessageLayer(SocketLayer socketLayer,
                        BlockingQueue<ByteBuffer> receivedMessages,
                        BlockingQueue<Message> decodedMessages
    ) {
        this.socketLayer = socketLayer;

        this.receivedMessages = receivedMessages;
        this.decodedMessages = decodedMessages;
        this.receivedMessages = new LinkedBlockingQueue<ByteBuffer>(); // TODO temp
    }


    public void send(Communication<Message> message) throws IOException {
        socketLayer.send(new Communication<>(
                message.getData().toByteString().asReadOnlyByteBuffer(),
                message.getPeer()
        ));
    }

    @Override
    public void run() {
        while (true) {
            try {
                ByteBuffer bytes = receivedMessages.take();
                decodedMessages.offer(Message.parseFrom(bytes));
            } catch (InterruptedException e) {
                // TODO quit ?
                e.printStackTrace();
            } catch (InvalidProtocolBufferException e) {
                logger.trace("invalid message, ignoring");
            }
        }
    }
}
