package botnet_p2p.test_node;

import botnet_p2p.Client;
import botnet_p2p.MessageReceiver;
import botnet_p2p.NodeManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;


public class TestNode {
    private static final Logger logger = LogManager.getLogger(TestNode.class);
    private Client client;


    TestNode(MessageReceiver messageReceiver,
             NodeManager nodeManager) throws IOException, InterruptedException {
        Runtime.getRuntime().addShutdownHook(new ShutdownHandler());
        CountDownLatch initLatch = new CountDownLatch(1);
        client = new Client(initLatch, messageReceiver, nodeManager);
        client.start();
        initLatch.await();

        Message.Builder builder = Message.newBuilder();
        builder.setType(Message.MessageType.PING);
        builder.setSender("sender name");
        Message message = builder.build();

        client.sendMessage(message, "127.0.0.1", 3000);
    }

    public static void main(String args[]) {
        logger.info("starting");
        ApplicationContext context = new AnnotationConfigApplicationContext(TestNodeConfig.class);
        TestNode testNode = context.getBean(TestNode.class);
        logger.info("started");

    }

    class ShutdownHandler extends Thread {
        @Override
        public void run() {
            super.run();
            logger.info("closing requested");
            client.interrupt();
        }
    }
}
