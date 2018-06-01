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

    private static ApplicationLayer createNode(KademliaPeer me, Peer bootstrapNode) throws InterruptedException, IOException {
        BlockingQueue<ByteBuffer> receivedMessages = new LinkedBlockingQueue<>(); // coming from the world
        BlockingQueue<Message> decodedMessages = new LinkedBlockingQueue<>(); // coming from the world, decoded
        CountDownLatch initLatch = new CountDownLatch(1);

        SocketLayer socketLayer = new SocketLayer(receivedMessages, initLatch, me.getPort());
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
        // args: me host:port, bootstrap host:port

        String[] meArgs = args[0].split(":");
        String[] bootstrapArgs = args[1].split(":");

        KademliaPeer me = new KademliaPeer(meArgs[0], Integer.parseInt(meArgs[1]));
        Peer boostrapNode = new Peer(bootstrapArgs[0], Integer.parseInt(bootstrapArgs[1]));

        logger.info("Hi, I'm " + me.toString());
        ApplicationLayer node = createNode(me, boostrapNode);
    }
}
