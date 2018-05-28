package botnet_p2p;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class NodeManager {
    private List<BotnetNode> nodes;

    public NodeManager() {
        this.nodes = new ArrayList<>();

    }

    public Stream<BotnetNode> getByStatus(NodeStatus waitingForConnect) {
        return nodes.stream().filter(botnetNode -> botnetNode.status == NodeStatus.WAITING_FOR_CONNECT);
    }

    public void add(BotnetNode botnetNode) {
        nodes.add(botnetNode);
    }

    public Optional<BotnetNode> getConnectedNodeByAddress(InetSocketAddress destAddress) {
        return nodes.stream().filter(
                botnetNode -> {
                    InetSocketAddress remoteAddress = botnetNode.address;
                    return remoteAddress.equals(destAddress);
                }).filter(botnetNode -> botnetNode.status == NodeStatus.CONNECTED).findFirst();
    }

    public Optional<BotnetNode> getNodeByAddress(InetSocketAddress destAddress) {
        return nodes.stream().filter(
                botnetNode -> {
                    InetSocketAddress remoteAddress = botnetNode.address;
                    return remoteAddress.equals(destAddress);
                }).findFirst();
    }
}
