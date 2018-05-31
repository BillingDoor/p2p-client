package botnet_p2p.kademlia;

import java.nio.channels.SocketChannel;

import static botnet_p2p.MessageOuterClass.Message;

public interface MessageListener {
    void foundNodesMessageReceived(Message message);

    void pingMessageReceived(Message message, SocketChannel sender);

    void findNodeMessageReceived(Message message, SocketChannel sender);
}
