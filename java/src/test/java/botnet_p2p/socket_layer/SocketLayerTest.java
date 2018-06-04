package botnet_p2p.socket_layer;

import botnet_p2p.model.Communication;
import botnet_p2p.model.Peer;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.*;

import static botnet_p2p.MessageOuterClass.Message;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class SocketLayerTest {

    private static final int PORT_SENDER = 3000;
    private static final int PORT_RECEIVER = 8080;

    private final Peer dstPeer = new Peer("127.0.0.1", PORT_RECEIVER);

    private final Communication<Message> communicationMessage = new Communication<>(
            Message.newBuilder()
                    .setType(Message.MessageType.PING)
                    .setUuid(UUID.randomUUID().toString())
                    .build(),
            dstPeer
    );

    private final Communication<ByteBuffer> communicationByteBuffer = new Communication<>(
            communicationMessage.getData().toByteString().asReadOnlyByteBuffer(),
            dstPeer
    );

    @Test
    public void sendMessage() throws IOException, InterruptedException {
        Semaphore received = new Semaphore(0);
        Thread socketsThread = new Thread(() -> {
            CountDownLatch initLatch = new CountDownLatch(1);
            SocketLayer socketLayer = new SocketLayer(null, initLatch, PORT_SENDER);
            socketLayer.start();
            try {
                initLatch.await();
                socketLayer.send(communicationByteBuffer);
                received.acquire();
                socketLayer.shutdown();
            } catch (InterruptedException e) {
                socketLayer.shutdown();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        socketsThread.start();

        // wait for connection from node
        ServerSocket serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress("127.0.0.1", PORT_RECEIVER));
        Socket socketRec = serverSocket.accept();

        // receive data
        BufferedInputStream bufferedInputStream = new BufferedInputStream(socketRec.getInputStream());
        byte[] buffer = new byte[2048];
        int read = bufferedInputStream.read(buffer);
        received.release();
        ByteBuffer messageBuffer = ByteBuffer.wrap(buffer, 0, read);
        int size = messageBuffer.getInt();
        Message receivedMessage = Message.parseFrom(messageBuffer);

        assertThat(receivedMessage).isNotNull();
        assertThat(receivedMessage).isEqualTo(communicationMessage.getData());
        assertThat(size).isEqualTo(communicationMessage.getData().getSerializedSize());

        socketsThread.join();
    }

    @Test
    public void receiveMessage() throws IOException, InterruptedException {
        BlockingQueue<ByteBuffer> receivedMessages = new LinkedBlockingQueue<>();
        CountDownLatch initLatch = new CountDownLatch(1);
        SocketLayer socketLayer = new SocketLayer(receivedMessages, initLatch, PORT_SENDER);
        socketLayer.start();
        initLatch.await();

        // socket that sends command
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", PORT_SENDER));
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(socket.getOutputStream());

        // initialize test peer and send message
        int size = communicationByteBuffer.getData().limit();
        ByteBuffer data = ByteBuffer.allocate(size + 4);
        data.putInt(size);
        data.put(communicationByteBuffer.getData());

        bufferedOutputStream.write(data.array());
        bufferedOutputStream.flush();


        ByteBuffer receivedData = receivedMessages.poll(1, TimeUnit.SECONDS);
        socketLayer.shutdown();
        assertThat(receivedData).isNotNull();

        Message receivedMessage = Message.parseFrom(receivedData);
        assertThat(receivedMessage).isEqualTo(communicationMessage.getData());
    }

    @Test
    public void sendAndReceiveMessage() throws InterruptedException, InvalidProtocolBufferException {
        // receiving
        BlockingQueue<ByteBuffer> receivedMessages = new LinkedBlockingQueue<>();
        CountDownLatch initLatch = new CountDownLatch(1);
        SocketLayer socketLayer = new SocketLayer(receivedMessages, initLatch, PORT_RECEIVER);
        socketLayer.start();
        initLatch.await();

        // sending
        Semaphore received = new Semaphore(0);
        Thread socketsThread = new Thread(() -> {
            CountDownLatch initLatch2 = new CountDownLatch(1);
            SocketLayer socketLayer2 = new SocketLayer(null, initLatch2, PORT_SENDER);
            socketLayer2.start();
            try {
                initLatch2.await();
                socketLayer2.send(communicationByteBuffer);
                received.acquire();
                socketLayer2.shutdown();
            } catch (InterruptedException e) {
                socketLayer2.shutdown();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        socketsThread.start();


        ByteBuffer receivedData = receivedMessages.poll(1, TimeUnit.SECONDS);
        received.release();
        socketLayer.shutdown();
        assertThat(receivedData).isNotNull();

        Message receivedMessage = Message.parseFrom(receivedData);
        assertThat(receivedMessage).isEqualTo(communicationMessage.getData());

        socketsThread.join();
    }
}