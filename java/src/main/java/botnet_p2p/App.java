package botnet_p2p;

import botnet_p2p.application_layer.ApplicationLayer;
import botnet_p2p.business_logic_layer.BusinessLogicLayer;
import botnet_p2p.message_layer.MessageLayer;
import botnet_p2p.model.KademliaPeer;
import botnet_p2p.model.Peer;
import botnet_p2p.p2p_layer.P2pLayer;
import botnet_p2p.socket_layer.SocketLayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import static botnet_p2p.MessageOuterClass.Message;

public class App {
    private static final Logger logger = LogManager.getLogger(App.class);

    private static ApplicationLayer createNode(String host, int port, Peer bootstrapNode) throws InterruptedException, IOException {
        KademliaPeer me = new KademliaPeer(host, port);
        BlockingQueue<ByteBuffer> receivedMessages = new LinkedBlockingQueue<>(); // coming from the world
        BlockingQueue<Message> decodedMessages = new LinkedBlockingQueue<>(); // coming from the world, decoded
        CountDownLatch initLatch = new CountDownLatch(1);

        SocketLayer socketLayer = new SocketLayer(receivedMessages, initLatch);
        socketLayer.start();
        initLatch.await();

        MessageLayer messageLayer = new MessageLayer(socketLayer, receivedMessages, decodedMessages);
        messageLayer.start();

        P2pLayer p2pLayer = new P2pLayer(messageLayer, decodedMessages, me);

        BusinessLogicLayer businessLogicLayer = new BusinessLogicLayer(p2pLayer, me);

        ApplicationLayer applicationLayer = new ApplicationLayer(businessLogicLayer);


        class ShutdownHandler extends Thread {
            @Override
            public void run() {
                super.run();
                logger.info("closing requested");
                socketLayer.interrupt(); // TODO
            }
        }
        Runtime.getRuntime().addShutdownHook(new ShutdownHandler());

        applicationLayer.launchClient(bootstrapNode);
        return applicationLayer;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Peer boostrapNode = new Peer("127.0.0.1", 8080);
        ApplicationLayer node1 = createNode("127.0.0.1", 3000, boostrapNode);
    }
}
