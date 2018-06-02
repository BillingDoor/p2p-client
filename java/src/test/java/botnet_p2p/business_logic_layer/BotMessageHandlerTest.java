package botnet_p2p.business_logic_layer;

import org.junit.Test;

import static botnet_p2p.MessageOuterClass.Message;
import static org.junit.Assert.assertThat;


public class BotMessageHandlerTest {

    public Message createCommandMessage(String command) {
        return Message.newBuilder()
                .setCommand(
                        Message.CommandMsg.newBuilder()
                                .setCommandString(command)
                                .build()
                ).build();
    }


    @Test
    public void testDirCommand() {
        BotMessageHandler botMessageHandler = new BotMessageHandler();
        String result = botMessageHandler.executeCommand("dir");

        assertThat(result, org.hamcrest.core.StringContains.containsString("Volume in drive"));
    }
}