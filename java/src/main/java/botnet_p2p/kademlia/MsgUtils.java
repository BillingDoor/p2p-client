package botnet_p2p.kademlia;

import botnet_p2p.MessageOuterClass.Message;

import java.util.List;
import java.util.stream.Collectors;

public class MsgUtils {

    public static Message.Builder createBase(KademliaPeer sender) {
        return Message.newBuilder()
                .setSender(sender.getAddress() + ":" + sender.getPort())
                .setUuid(sender.getId());
    }

    public static Message createFoundNodes(KademliaPeer sender, List<KademliaPeer> nearestPeers) {
        List<Message.NodeDescription> nodeDescriptions = nearestPeers
                .stream()
                .map(kademliaPeer -> {
                    return Message.NodeDescription.newBuilder()
                            .setGuid(kademliaPeer.getId())// TODO isNAT
                            .setIP(kademliaPeer.getAddress())
                            .setPort(String.valueOf(kademliaPeer.getPort()))
                            .build();
                }).collect(Collectors.toList());
        Message.FoundNodes foundNodes = Message.FoundNodes.newBuilder().addAllNodes(nodeDescriptions).build();
        return createBase(sender)
                .setType(Message.MessageType.FOUND_NODES)
                .setPFoundNodes(foundNodes)
                .build();
    }

    public static List<KademliaPeer> getNodeDescriptionsAsPeers(List<Message.NodeDescription> nodeDescriptions) {
        return nodeDescriptions
                .stream()
                .map(nodeDescription ->
                        new KademliaPeer(
                                nodeDescription.getIP(),
                                Integer.parseInt(nodeDescription.getPort()),
                                nodeDescription.getGuid()))
                .collect(Collectors.toList());
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

    public static Message createFindNode(KademliaPeer sender) {
        return createBase(sender)
                .setType(Message.MessageType.FIND_NODE)
                .setPFindNode(Message.FindNode.newBuilder().setGuid(sender.getId()))
                .build();
    }
}
