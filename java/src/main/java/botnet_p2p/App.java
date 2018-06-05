package botnet_p2p;

import botnet_p2p.application_layer.ApplicationLayer;
import botnet_p2p.application_layer.Config;
import botnet_p2p.model.Peer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class App {
    private static final Logger logger = LogManager.getLogger(App.class);

    public static void main(String[] args) {

        ApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
        ApplicationLayer applicationLayer = context.getBean(ApplicationLayer.class);

        class ShutdownHandler extends Thread {
            @Override
            public void run() {
                super.run();
                logger.info("closing requested");
                applicationLayer.shutdown();
            }
        }
        Runtime.getRuntime().addShutdownHook(new ShutdownHandler());

        if (System.getProperty("bootstrap") != null) {
            applicationLayer.launchClient((Peer) context.getBean("bootstrap"));
        } else {
            applicationLayer.startWithoutBootstrapping();
        }

    }
}
