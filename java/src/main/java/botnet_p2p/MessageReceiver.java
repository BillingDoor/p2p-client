package botnet_p2p;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;

public class MessageReceiver {
    private static final Logger logger = LogManager.getLogger(MessageReceiver.class);

    private MessageHandler messageHandler;

    public MessageReceiver(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    public void handleNewMessage(SelectableChannel channel) throws IOException {
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
        Message message = Message.parseFrom(messageBuffer);

        inputBuffer.clear();

        logger.info("message parsed");
        logger.info("message content:\r\n" + message.toString());
        messageHandler.handle(message, client);
    }
}
