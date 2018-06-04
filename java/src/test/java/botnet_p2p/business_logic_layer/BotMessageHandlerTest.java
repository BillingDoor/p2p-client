package botnet_p2p.business_logic_layer;

import botnet_p2p.model.KademliaPeer;
import botnet_p2p.p2p_layer.P2pLayer;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static botnet_p2p.MessageOuterClass.Message;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;


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
    public void testDirCommand() throws IOException, InterruptedException {
        Runtime runtime = mock(Runtime.class);
        Process process = mock(Process.class);
        when(process.waitFor()).thenReturn(0);
        when(process.exitValue()).thenReturn(-1);
        when(runtime.exec("cmd /c dir C:\\botnet")).thenReturn(process);

        P2pLayer p2pLayer = mock(P2pLayer.class);
        KademliaPeer me = new KademliaPeer("127.0.0.1", 3000);
        BotMessageHandler botMessageHandler = new BotMessageHandler(p2pLayer, null, me, runtime);


        String result = botMessageHandler.executeSystemCommand("dir");

        assertThat(result).isNull();
    }

    @Test
    public void testDirCommandMessage() throws InterruptedException, IOException {
        Runtime runtime = mock(Runtime.class);
        Process process = mock(Process.class);
        when(process.waitFor()).thenReturn(0);
        when(process.exitValue()).thenReturn(-1);
        when(runtime.exec("cmd /c dir C:\\botnet")).thenReturn(process);
        P2pLayer p2pLayer = mock(P2pLayer.class);
        KademliaPeer me = new KademliaPeer("127.0.0.1", 3000);
        BotMessageHandler botMessageHandler = new BotMessageHandler(p2pLayer, null, me, runtime);

        botMessageHandler.handleCommandMessage(createCommandMessage("dir", true));

        Mockito.verify(p2pLayer).commandResponse(anyObject(), eq(me), eq(""), eq(false));
    }

    @Test
    public void testDirCommandMessageNoResponseRequested() throws InterruptedException, IOException {
        Runtime runtime = mock(Runtime.class);
        Process process = mock(Process.class);
        when(process.waitFor()).thenReturn(0);
        when(process.exitValue()).thenReturn(-1);
        when(runtime.exec("cmd /c dir C:\\botnet")).thenReturn(process);
        P2pLayer p2pLayer = mock(P2pLayer.class);
        KademliaPeer me = new KademliaPeer("127.0.0.1", 3000);
        BotMessageHandler botMessageHandler = new BotMessageHandler(p2pLayer, null, me, runtime);

        botMessageHandler.handleCommandMessage(createCommandMessage("dir", false));

        Mockito.verify(p2pLayer, never()).commandResponse(anyObject(), anyObject(), anyObject(), anyBoolean());
    }

    @Test
    public void getFileName_1() {
        Runtime runtime = mock(Runtime.class);
        BotMessageHandler botMessageHandler = new BotMessageHandler(null, null, null, null);

        String fileName = botMessageHandler.getFileName("/aa/bvv/c.txt");

        assertThat(fileName).isEqualToIgnoringCase("c.txt");
    }

    @Test
    public void getFileName_2() {
        BotMessageHandler botMessageHandler = new BotMessageHandler(null, null, null, null);

        String fileName = botMessageHandler.getFileName("C:\\aa\\bvv\\c.txt");

        assertThat(fileName).isEqualToIgnoringCase("c.txt");
    }
}