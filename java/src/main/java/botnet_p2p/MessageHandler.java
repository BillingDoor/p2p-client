package botnet_p2p;

public class MessageHandler {

    void handle(MessageOuterClass.Message message) {
        switch (message.getType()) {
            case PING:
                // ask what to do
                // return message to send
                // respond
                break;
            default:
                throw new RuntimeException("message type unsupported");
        }
    }
}
