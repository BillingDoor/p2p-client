package botnet_p2p.p2p_layer;

import botnet_p2p.message_layer.MessageLayer;
import botnet_p2p.model.Communication;
import botnet_p2p.model.KademliaPeer;
import botnet_p2p.model.Peer;
import botnet_p2p.protobuf_layer.Protobuf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import static botnet_p2p.MessageOuterClass.Message;


public class P2pLayer {
    private static final Logger logger = LogManager.getLogger(P2pLayer.class);

    private MessageLayer messageLayer;
    private BlockingQueue<Message> decodedMessages;

    private BucketsList routingTable;

    public P2pLayer(MessageLayer messageLayer,
                    BlockingQueue<Message> decodedMessages,
                    KademliaPeer me) {
        this.messageLayer = messageLayer;
        this.decodedMessages = decodedMessages;

        this.routingTable = new BucketsList(64, 20, me.getGuid());
    }


    public BlockingQueue<Message> getDecodedMessagesQueue() {
        return decodedMessages;
    }

    public BucketsList getRoutingTable() {
        return routingTable;
    }

    public void ping(KademliaPeer destination, KademliaPeer me) {
        messageLayer.send(
                new Communication<>(
                        Protobuf.createPingMessage(destination, me),
                        destination.toPeer()
                ));

    }

    public void findNode(Peer bootstrapNode, KademliaPeer me) {
        messageLayer.send(
                new Communication<>(
                        Protobuf.createFindNodeMessage(bootstrapNode, me),
                        bootstrapNode
                ));
    }

    public void pingResponse(KademliaPeer destination, KademliaPeer me) {
        messageLayer.send(
                new Communication<>(
                        Protobuf.createPingResponseMessage(destination, me),
                        destination.toPeer()
                ));
    }

    public void foundNodes(KademliaPeer destination, KademliaPeer me, List<KademliaPeer> nearestPeers) {
        messageLayer.send(
                new Communication<>(
                        Protobuf.createFoundNodesMessage(destination, me, nearestPeers),
                        destination.toPeer()
                ));
    }

    public void leave(KademliaPeer sender, KademliaPeer destination) {
        messageLayer.send(
                new Communication<>(
                        Protobuf.createLeaveMessage(sender, destination),
                        destination.toPeer()
                ));
    }

    public void addToRoutingTable(KademliaPeer peer) {
        this.routingTable.insert(peer);
    }

    public void removeFromRoutingTable(KademliaPeer kademliaPeer) {
        boolean removed = this.routingTable.remove(kademliaPeer);
        logger.trace("removed from routing table " + removed);
    }

    public List<KademliaPeer> getNearestPeers(String guid) {
        return this.routingTable.getNearestPeers(guid);
    }

    public List<KademliaPeer> getPeers() {
        return this.routingTable.getPeers();
    }

    public void shutdown() {
        logger.info("closing");
        this.messageLayer.shutdown();
    }


}
