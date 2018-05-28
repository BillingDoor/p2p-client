package botnet_p2p;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Botnet {

    private static final Logger logger = LogManager.getLogger(Server.class);
    private Server server;

    private MessageReceiver messageReceiver;


    Botnet(int port, MessageReceiver messageReceiver, NodeManager nodeManager) {
        Runtime.getRuntime().addShutdownHook(new ShutdownHandler());
        server = new Server(port, messageReceiver, nodeManager);
        server.start();
    }

    public static void main(String args[]) {
        logger.info("starting");
        ApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
        Botnet botnet = context.getBean(Botnet.class);
        logger.info("started");
    }

    class ShutdownHandler extends Thread {
        @Override
        public void run() {
            super.run();
            logger.info("closing requested");
            server.interrupt();
        }
    }



}