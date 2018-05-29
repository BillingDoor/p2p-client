package botnet_p2p.kademlia;

import botnet_p2p.MessageOuterClass;

import java.nio.channels.SocketChannel;

public interface MessageListener {
    void foundNodesMessageReceived(MessageOuterClass.Message message);

    void pingMessageReceived(MessageOuterClass.Message message, SocketChannel sender);

    void findNodeMessageReceived(MessageOuterClass.Message message, SocketChannel sender);
}
