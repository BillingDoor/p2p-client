package botnet_p2p.message_layer;

import botnet_p2p.model.Communication;
import botnet_p2p.model.Peer;
import botnet_p2p.socket_layer.SocketLayer;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static botnet_p2p.MessageOuterClass.Message;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MessageLayerTest {

    private final Peer peer = new Peer("127.0.0.1", 8080);
    private final Communication<Message> communicationMessage = new Communication<>(
            Message.newBuilder()
                    .setType(Message.MessageType.PING)
                    .setUuid("xyz")
                    .build(),
            peer
    );

    private final Communication<ByteBuffer> communicationByteBuffer = new Communication<>(
            communicationMessage.getData().toByteString().asReadOnlyByteBuffer(),
            peer
    );

    @Test
    public void sendMessage() throws IOException {
        SocketLayer socketLayer = mock(SocketLayer.class);
        MessageLayer messageLayer = new MessageLayer(socketLayer, null, null);
        ArgumentCaptor<Communication<ByteBuffer>> argument = ArgumentCaptor.forClass(Communication.class);

        messageLayer.send(communicationMessage);

        verify(socketLayer).send(argument.capture());
        assertThat(argument.getValue().getPeer()).isEqualToComparingFieldByFieldRecursively(peer);
        assertThat(argument.getValue()).isEqualTo(communicationByteBuffer);
    }


    @Test
    public void decode() throws InterruptedException {
        BlockingQueue<ByteBuffer> receivedMessages = new LinkedBlockingQueue<>();

        BlockingQueue<Message> decodedMessages = new LinkedBlockingQueue<>();
        MessageLayer messageLayer = new MessageLayer(
                null,
                receivedMessages,
                decodedMessages
        );

        receivedMessages.offer(communicationByteBuffer.getData());
        messageLayer.start();

        Message message = decodedMessages.poll(1, TimeUnit.SECONDS);
        assertThat(message).isNotNull();
        assertThat(message).isEqualTo(communicationMessage.getData());
    }

    @Test
    public void sendAndDecode() throws InterruptedException, IOException {
        SocketLayer socketLayer = mock(SocketLayer.class);
        BlockingQueue<ByteBuffer> receivedMessages = new LinkedBlockingQueue<>();
        BlockingQueue<Message> decodedMessages = new LinkedBlockingQueue<>();
        MessageLayer messageLayer = new MessageLayer(
                socketLayer,
                receivedMessages,
                decodedMessages
        );

        messageLayer.send(communicationMessage);
        ArgumentCaptor<Communication<ByteBuffer>> argument = ArgumentCaptor.forClass(Communication.class);

        verify(socketLayer).send(argument.capture());
        assertThat(argument.getValue().getPeer()).isEqualToComparingFieldByFieldRecursively(peer);



        receivedMessages.offer(argument.getValue().getData());
        messageLayer.start();
        Message message = decodedMessages.poll(1, TimeUnit.SECONDS);

        assertThat(message).isNotNull();
        assertThat(message).isEqualTo(communicationMessage.getData());
    }
}