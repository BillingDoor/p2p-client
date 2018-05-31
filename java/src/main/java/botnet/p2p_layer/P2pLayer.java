package botnet.p2p_layer;

import botnet.message_layer.MessageLayer;
import botnet.model.Communication;
import botnet.model.KademliaPeer;
import botnet.model.Peer;
import botnet.protobuf_layer.Protobuf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

import static botnet_p2p.MessageOuterClass.Message;


public class P2pLayer {
    private static final Logger logger = LogManager.getLogger(P2pLayer.class);

    private Protobuf protobuf;
    private MessageLayer messageLayer;
    private BlockingQueue<Message> decodedMessages;

    private BucketsList routingTable;

    public P2pLayer(Protobuf protobuf,
                    MessageLayer messageLayer,
                    BlockingQueue<Message> decodedMessages) {
        this.messageLayer = messageLayer;
        this.protobuf = protobuf;

        this.decodedMessages = decodedMessages;
    }

    public void findNode(Peer bootstrapNode, KademliaPeer me) throws IOException {
        messageLayer.send(
                new Communication<>(
                        protobuf.createFindNodeMessage(bootstrapNode, me),
                        bootstrapNode
                ));
    }

    public BlockingQueue<Message> getDecodedMessagesQueue() {
        return decodedMessages;
    }

    public BucketsList getRoutingTable() {
        return routingTable;
    }
}
