package botnet_p2p.kademlia;

import botnet_p2p.MessageOuterClass.Message;

public class Messages {

    public static Message.Builder getBase(KademliaPeer sender) {
        return Message.newBuilder()
                .setSender(sender.getAddress() + ":" + sender.getPort())
                .setUuid(sender.getId());
    }
}
