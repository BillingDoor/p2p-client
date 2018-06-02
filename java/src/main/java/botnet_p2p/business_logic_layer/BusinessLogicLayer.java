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
import java.util.concurrent.TimeUnit;

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
    private KadMessageHandler kadMessageHandler;
    private BotMessageHandler botMessageHandler;
    private Semaphore bootstrapFinish;

    public BusinessLogicLayer(P2pLayer p2pLayer, KademliaPeer me, KadMessageHandler kadMessageHandler) {
        this.p2pLayer = p2pLayer;
        this.me = me;

        decodedMessagesQueue = p2pLayer.getDecodedMessagesQueue();
        this.kadMessageHandler = kadMessageHandler;
        this.botMessageHandler = new BotMessageHandler();
        this.bootstrapFinish = new Semaphore(0);
        logger.info("Hi, I'm " + me.toString());
    }

    public BusinessLogicLayer(P2pLayer p2pLayer, KademliaPeer me) {
        this.p2pLayer = p2pLayer;
        this.me = me;

        decodedMessagesQueue = p2pLayer.getDecodedMessagesQueue();
        this.kadMessageHandler = new KadMessageHandler(p2pLayer, me);
        this.botMessageHandler = new BotMessageHandler();
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

    public void sendFileTo() {

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
        long pingResponseStart = 0;
        boolean pingsSent = false;
        List<KademliaPeer> pingedNodes = new ArrayList<>();

        while (true) {
            if (System.currentTimeMillis() > foundNodesStart) {
                logger.error("bootstrap node did not respond, timeout");
                break;
            }

            try {
                Message message = decodedMessagesQueue.poll(1, TimeUnit.SECONDS);
                if (message == null) {
                    continue;
                }
                logger.info("decoded message:\n" + message.toString());

                switch (message.getType()) {
                    case FOUND_NODES:
                        KademliaPeer sender = KademliaPeer.fromContact(message.getSender());
                        if (!sender.equalsTo(bootstrapNode)) {
                            logger.error("found nodes msg does not come from bootstrapNode");
                            break;
                        }
                        kadMessageHandler.handleFoundNodes(message, pingedNodes);

                        // wait for ping responses
                        pingsSent = true;
                        pingResponseStart = System.currentTimeMillis() + BOOTSTRAP_PING_RESPONSE_TIMEOUT * 1000;
                        logger.info("waiting for ping responses");
                        break;
                    case PING_RESPONSE:
                        kadMessageHandler.handlePingResponse(message, pingedNodes);
                        break;
                    case PING:
                        kadMessageHandler.handlePingMessage(message);
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
                        kadMessageHandler.handlePingMessage(messsage);
                        break;
                    case FIND_NODE:
                        kadMessageHandler.handleFindNode(messsage);
                        break;
                    case LEAVE:
                        kadMessageHandler.handleLeaveMessage(messsage);
                        break;
                    case COMMAND:
                        botMessageHandler.handleCommandMessage(messsage);
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
