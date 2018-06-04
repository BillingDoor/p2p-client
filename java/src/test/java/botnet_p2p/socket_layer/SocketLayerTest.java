package botnet_p2p.socket_layer;

import botnet_p2p.model.Communication;
import botnet_p2p.model.Peer;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import static botnet_p2p.MessageOuterClass.Message;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class SocketLayerTest {

    private static final int PORT_SENDER = 3000;
    private static final int PORT_RECEIVER = 8080;

    private final Peer dstPeer = new Peer("127.0.0.1", PORT_RECEIVER);

    private final Communication<Message> communicationMessage = new Communication<>(
            Message.newBuilder()
                    .setType(Message.MessageType.PING)
                    .setUuid("xyz")
                    .build(),
            dstPeer
    );

    Communication<ByteBuffer> communicationByteBuffer = new Communication<>(
            communicationMessage.getData().toByteString().asReadOnlyByteBuffer(),
            dstPeer
    );

    @Test
    public void sendMessage() throws IOException, InterruptedException {
        new Thread(() -> {
            CountDownLatch initLatch = new CountDownLatch(1);
            SocketLayer socketLayer = new SocketLayer(null, initLatch, PORT_SENDER);
            socketLayer.start();
            try {
                initLatch.await();
                socketLayer.send(communicationByteBuffer);
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }).start();

        // wait for connection from node
        ServerSocket serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress("127.0.0.1", PORT_RECEIVER));
        Socket socketRec = serverSocket.accept();

        // receive data
        BufferedInputStream bufferedInputStream = new BufferedInputStream(socketRec.getInputStream());
        byte[] buffer = new byte[2048];
        int read = bufferedInputStream.read(buffer);
        ByteBuffer messageBuffer = ByteBuffer.wrap(buffer, 0, read);
        Message receivedMessage = Message.parseFrom(messageBuffer);

        assertThat(receivedMessage).isNotNull();
        assertThat(receivedMessage).isEqualTo(communicationMessage.getData());
    }


}