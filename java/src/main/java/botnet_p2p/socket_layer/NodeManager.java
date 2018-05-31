package botnet_p2p.socket_layer;


import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

class NodeManager {
    private List<Node> nodes;

    public NodeManager() {
        this.nodes = Collections.synchronizedList(new ArrayList<>());

    }

    Stream<Node> getByStatus(NodeStatus waitingForConnect) {
        return nodes.stream().filter(botnetNode -> botnetNode.status == NodeStatus.WAITING_FOR_CONNECT);
    }

    public void add(Node node) {
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
}
