package botnet_p2p.business_logic_layer;

import botnet_p2p.model.KademliaPeer;
import botnet_p2p.model.Peer;
import botnet_p2p.p2p_layer.P2pLayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

import static botnet_p2p.MessageOuterClass.Message;


public class BusinessLogicLayer extends Thread {
    private static final Logger logger = LogManager.getLogger(BusinessLogicLayer.class);
    private static final int BOOTSTRAP_FOUND_NODES_RESPONSE_TIMEOUT = 8;
    private static final int BOOTSTRAP_PING_RESPONSE_TIMEOUT = 8;
    private boolean doBootstrap = false;
    private P2pLayer p2pLayer;
    private KademliaPeer me;
    private Peer bootstrapNode;
    private BlockingQueue<Message> decodedMessagesQueue;
    private MessageHandler messageHandler;
    private Semaphore bootstrapFinish;

    public BusinessLogicLayer(P2pLayer p2pLayer, KademliaPeer me) {
        this.p2pLayer = p2pLayer;
        this.me = me;

        decodedMessagesQueue = p2pLayer.getDecodedMessagesQueue();
        this.messageHandler = new MessageHandler(p2pLayer, me);
        this.bootstrapFinish = new Semaphore(0);
    }

    public void joinNetwork(Peer bootstrapNode) {
        this.bootstrapNode = bootstrapNode;
        this.doBootstrap = true;
        this.start();
        p2pLayer.findNode(bootstrapNode, me);

        try {
            bootstrapFinish.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void createNetwork() {
        this.doBootstrap = false;
        this.start();
    }


    @Override
    public void run() {
        if (doBootstrap) {
            // pre bootstrap
            logger.info("starting pre bootstrap loop");
            preBootstrapLoop();
        }

        // post bootstrap
        logger.info("starting post bootstrap loop");
        bootstrapFinish.release();
        postBootstrapLoop();
        logger.info("closing - loop ended");
    }

    private void preBootstrapLoop() {
        long foundNodesStart = System.currentTimeMillis() + BOOTSTRAP_FOUND_NODES_RESPONSE_TIMEOUT * 1000;
        while (true) {
            if (System.currentTimeMillis() > foundNodesStart) {
                logger.error("bootstrap node did not respond, timeout");
                break;
            }

            long pingResponseStart = 0;
            boolean pingsSent = false;
            List<KademliaPeer> pingedNodes = new ArrayList<>();

            try {
                Message message = decodedMessagesQueue.take();
                logger.info("decoded message:\n" + message.toString());

                KademliaPeer sender = KademliaPeer.fromContact(message.getSender());


                switch (message.getType()) {
                    case FOUND_NODES:
                        if (!sender.equalsTo(bootstrapNode)) {
                            logger.error("found nodes msg does not come from bootstrapNode");
                            return;
                        }
                        // bootstrapNode responded, adding it to routing table
                        messageHandler.addToRoutingTable(sender);

                        message.getFoundNodes().getNodesList().forEach(contact -> {
                            if (!contact.getGuid().equals(me.getGuid())) {
                                // pinging nodes that we got from bootstrapNode
                                KademliaPeer peer = KademliaPeer.fromContact(contact);
                                logger.info("pinging node: " + peer.getGuid());
                                p2pLayer.ping(peer, me);
                                pingedNodes.add(peer);
                            }
                        });

                        // wait for ping responses
                        pingsSent = true;
                        pingResponseStart = System.currentTimeMillis() + BOOTSTRAP_PING_RESPONSE_TIMEOUT * 1000;
                        logger.info("waiting for ping responses");

                        break;
                    case PING_RESPONSE:
                        // peer responded, so it's alive - we can add it to routing table
                        messageHandler.addToRoutingTable(sender);
                        pingedNodes.remove(sender);
                        break;
                    case PING:
                        messageHandler.handlePingMessage(message);
                        break;
                    default:
                        logger.error("unsupported message type:" + message.getType());
                }

                // end bootstrap when all nodes responded or timeout
                if (pingsSent && pingedNodes.isEmpty() || System.currentTimeMillis() > pingResponseStart) {
                    logger.info("bootstrap process finished " + pingedNodes.size() + " nodes did not respond");
                    pingedNodes.clear();
                    break;
                }


            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void postBootstrapLoop() {
        while (true) {
            try {
                Message messsage = decodedMessagesQueue.take();
                logger.info("decoded message:\n" + messsage.toString());

                if (messsage.getSender() == null) {
                    logger.info("sender not defined in message, skipping");
                    continue;
                }

                switch (messsage.getType()) {
                    case PING:
                        messageHandler.handlePingMessage(messsage);
                        break;
                    case FIND_NODE:
                        messageHandler.handleFindNode(messsage);
                        break;
                    case LEAVE:
                        messageHandler.handleLeaveMessage(messsage);
                        break;
                    default:
                        logger.error("unsupported message type:" + messsage.getType());
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

    }

    public void shutdown() {
        p2pLayer.getPeers().forEach(
                peer -> p2pLayer.leave(me, peer)
        );
        logger.info("closing");
        this.interrupt();
        this.p2pLayer.shutdown();
    }

    public String getRoutingTable() {
        return this.p2pLayer.getPeers()
                .stream()
                .map(KademliaPeer::toString)
                .reduce("Routing table:\n\t", (a, b) -> a + "\n\t" + b);

    }
}
