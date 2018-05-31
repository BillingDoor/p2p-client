package botnet.application_layer;

import botnet.business_logic_layer.BusinessLogicLayer;
import botnet.model.Peer;
import botnet.p2p_layer.P2pLayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class ApplicationLayer {
    private static final Logger logger = LogManager.getLogger(P2pLayer.class);

    private BusinessLogicLayer businessLogicLayer;

    public ApplicationLayer(BusinessLogicLayer businessLogicLayer) {
        this.businessLogicLayer = businessLogicLayer;
    }

    public void launchClient() throws IOException {

        // blocking
        businessLogicLayer.joinNetwork(new Peer("127.0.0.1", 8080));
    }

}
