package botnet_p2p.socket_layer;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.stream.Stream;

class NodeManager {
    private static final Logger logger = LogManager.getLogger(NodeManager.class);

    private List<Node> nodes;

    NodeManager() {
        this.nodes = Collections.synchronizedList(new ArrayList<>());

    }

    Stream<Node> getByStatus(NodeStatus nodeStatus) {
        return nodes.stream().filter(botnetNode -> botnetNode.status == nodeStatus);
    }

    void add(Node node) {
        nodes.add(node);
    }

    Optional<Node> getConnectedNodeByAddress(InetSocketAddress destAddress) {
        return nodes.stream().filter(
                node -> {
                    InetSocketAddress remoteAddress = node.address;
                    return remoteAddress.equals(destAddress);
                }).filter(node -> node.status == NodeStatus.CONNECTED).findFirst();
    }

    Optional<Node> getNodeByAddress(InetSocketAddress destAddress) {
        return nodes.stream().filter(
                node -> {
                    InetSocketAddress remoteAddress = node.address;
                    return remoteAddress.equals(destAddress);
                }).findFirst();
    }

    void removeNode(SocketAddress remoteAddress) {
        Iterator<Node> it = this.nodes.iterator();
        while(it.hasNext()) {
            Node node = it.next();
            if(node.address.equals(remoteAddress)) {
                logger.info("node removed");
                it.remove();
                break;
            }
        }
    }
}
