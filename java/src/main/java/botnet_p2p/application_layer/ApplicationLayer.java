package botnet_p2p.application_layer;

import botnet_p2p.business_logic_layer.BusinessLogicLayer;
import botnet_p2p.model.Peer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Scanner;

public class ApplicationLayer {
    private static final Logger logger = LogManager.getLogger(ApplicationLayer.class);

    private BusinessLogicLayer businessLogicLayer;
    private boolean isShuttingDown = false;

    public ApplicationLayer(BusinessLogicLayer businessLogicLayer) {
        this.businessLogicLayer = businessLogicLayer;
    }

    public void launchClient(Peer bootstrapNode) {
        businessLogicLayer.joinNetwork(bootstrapNode);

        logger.info("bootstrap finished");
        readUserCommands();
    }

    public void startWithoutBootstrapping() {
        businessLogicLayer.createNetwork();

        readUserCommands();
    }

    public void shutdown() {
        if(!isShuttingDown) {
            logger.info("closing");
            isShuttingDown = true;
            businessLogicLayer.shutdown();
        }
    }

    private void printRoutingTable() {
        logger.info(
                businessLogicLayer.getRoutingTable()
        );
    }

    public void sendCommand(String command, String id) {
        businessLogicLayer.sendCommand(command, id);
    }

    private void readUserCommands() {
        // commands:
        // print routing - print routing table
        // command <command> <peer id> - send command to peer
        // file_request <path> <peer id> - request file from peer
        // ping <peer id> <propagate> // yes/no
        // exit
        Scanner scanner = new Scanner(System.in);

        while (true) {
            try {
                String command = scanner.nextLine();
                if ("print routing".equals(command)) {
                    this.printRoutingTable();
                } else if (command.startsWith("command")) {
                    String[] split = command.split(" ");
                    if (split.length != 3) {
                        logger.error("invalid command");
                        continue;
                    }
                    businessLogicLayer.sendCommand(split[1], split[2]);
                } else if (command.startsWith("file_request")) {
                    String[] split = command.split(" ");
                    if (split.length != 3) {
                        logger.error("invalid command");
                        continue;
                    }
                    businessLogicLayer.requestFile(split[1], split[2]);
                } else if (command.startsWith("ping")) {
                    String[] split = command.split(" ");
                    if (split.length != 3) {
                        logger.error("invalid command");
                        continue;
                    }
                    businessLogicLayer.sendPing(split[1], split[2]);
                } else if (command.startsWith("exit")) {
                    this.shutdown();
                    break;
                }
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
    }
}
