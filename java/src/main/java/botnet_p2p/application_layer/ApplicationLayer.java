package botnet_p2p.application_layer;

import botnet_p2p.business_logic_layer.BusinessLogicLayer;
import botnet_p2p.model.Peer;
import botnet_p2p.p2p_layer.P2pLayer;
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
