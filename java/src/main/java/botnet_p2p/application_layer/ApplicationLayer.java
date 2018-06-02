package botnet_p2p.application_layer;

import botnet_p2p.business_logic_layer.BusinessLogicLayer;
import botnet_p2p.model.Peer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ApplicationLayer {
    private static final Logger logger = LogManager.getLogger(ApplicationLayer.class);

    private BusinessLogicLayer businessLogicLayer;

    public ApplicationLayer(BusinessLogicLayer businessLogicLayer) {
        this.businessLogicLayer = businessLogicLayer;
    }

    public void launchClient(Peer bootstrapNode) {
        new Thread(() -> {
            // blocking
            try {
                businessLogicLayer.joinNetwork(bootstrapNode);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            logger.info("bootstrap finished");

            // non blocking
            businessLogicLayer.start();
        }).start();

        readCommands();
    }

    public void startWithoutBootstrapping() {
        businessLogicLayer.start();

        readCommands();
    }

    public void shutdown() {
        logger.info("closing");
        businessLogicLayer.shutdown();
    }

    private void printRoutingTable() {
        logger.info(
                businessLogicLayer.getRoutingTable()
        );
    }

    private void readCommands() {
        while (true) {
            String command = System.console().readLine();
            if("p".equals(command)) {
                this.printRoutingTable();
            }
        }
    }
}
