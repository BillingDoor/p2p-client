package botnet_p2p.application_layer;

import botnet_p2p.MessageOuterClass;
import botnet_p2p.business_logic_layer.BusinessLogicLayer;
import botnet_p2p.message_layer.MessageLayer;
import botnet_p2p.model.KademliaPeer;
import botnet_p2p.model.Peer;
import botnet_p2p.p2p_layer.P2pLayer;
import botnet_p2p.protobuf_layer.Protobuf;
import botnet_p2p.socket_layer.SocketLayer;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import static botnet_p2p.MessageOuterClass.Message;
import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;


public class ApplicationLayerTest {
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 3000;
    private final KademliaPeer me = new KademliaPeer(SERVER_HOST, SERVER_PORT);


    @Test
    public void bootstrapTest() throws IOException, InterruptedException {

        new Thread(() -> {
            try {
                // start node
                ApplicationLayer applicationLayer = buildApplicationLayer();

                sleep(500);
                Peer boostrapNode = new Peer("127.0.0.1", 123);
                applicationLayer.launchClient(boostrapNode);

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();


        // wait for connection from node
        ServerSocket serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress("127.0.0.1", 123));
        Socket socket = serverSocket.accept();

        // receive data
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(socket.getOutputStream());
        BufferedInputStream bufferedInputStream = new BufferedInputStream(socket.getInputStream());
        byte[] buffer = new byte[512];
        int read = bufferedInputStream.read(buffer);
        ByteBuffer messageBuffer = ByteBuffer.wrap(buffer, 0, read);
        Message receivedMessage = Message.parseFrom(messageBuffer);

        // check message
        assertEquals(receivedMessage.getType(), Message.MessageType.FIND_NODE);


        // send found nodes to node
        Message foundNodes = Protobuf.createFoundNodes(
                new KademliaPeer("127.0.0.1", 123, 44),
                me.toPeer(),
                Arrays.asList(
                        new KademliaPeer("127.11.11.1", 10, 0b01010101),
                        new KademliaPeer("127.11.11.12", 10, 0b00110101),
                        new KademliaPeer("127.11.11.13", 10, 0b00100101),
                        new KademliaPeer("127.11.11.14", 10, 0b01010111),
                        new KademliaPeer("127.11.11.15", 10, 0b10110101),
                        new KademliaPeer("127.11.11.16", 10, 0b00111101),
                        new KademliaPeer("127.11.11.17", 10, 0b00011101)
                )
        );
        foundNodes.writeTo(bufferedOutputStream);
        bufferedOutputStream.flush();

        // wait for bootstraping timeout
        Thread.sleep(12000);

        // ping to node
        Message pingMessage = Protobuf.createPingMessage(
                new KademliaPeer("127.0.0.1", 3000),
                new KademliaPeer("127.0.0.1", 123, 44)
        );
        pingMessage.writeTo(bufferedOutputStream);
        bufferedOutputStream.flush();

        // read response
        read = bufferedInputStream.read(buffer);
        messageBuffer = ByteBuffer.wrap(buffer, 0, read);
        receivedMessage = Message.parseFrom(messageBuffer);


        assertEquals(receivedMessage.getType(), Message.MessageType.PING_RESPONSE);
    }



    private ApplicationLayer buildApplicationLayer() throws InterruptedException, IOException {
        BlockingQueue<ByteBuffer> receivedMessages = new LinkedBlockingQueue<>(); // coming from the world
        BlockingQueue<MessageOuterClass.Message> decodedMessages = new LinkedBlockingQueue<>(); // coming from the world, decoded
        CountDownLatch initLatch = new CountDownLatch(1);

        SocketLayer socketLayer = new SocketLayer(receivedMessages, initLatch);
        socketLayer.start();
        initLatch.await();

        MessageLayer messageLayer = new MessageLayer(socketLayer, receivedMessages, decodedMessages);
        messageLayer.start();

        P2pLayer p2pLayer = new P2pLayer(messageLayer, decodedMessages, me);

        BusinessLogicLayer businessLogicLayer = new BusinessLogicLayer(p2pLayer, me);

        return new ApplicationLayer(businessLogicLayer);
    }
}