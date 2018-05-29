package botnet_p2p.kademlia;

import botnet_p2p.*;
import botnet_p2p.MessageOuterClass.Message;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertEquals;


public class KademliaNodeTest {

    private static final String KADEMLIA_SERVER_HOST = "127.0.0.1";
    private static final int KADEMLIA_SERVER_PORT = 3000;
    private static final int KADEMLIA_SERVER_UUID = 123;
    private static final int MAX_BUCKET_SIZE = 5;

    @Test
    public void pingTest() throws InterruptedException, IOException {
        KademliaNode kademliaNode = setupKademliaNode();

        // socket that sends ping message
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(KADEMLIA_SERVER_HOST, KADEMLIA_SERVER_PORT));
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(socket.getOutputStream());
        BufferedInputStream bufferedInputStream = new BufferedInputStream(socket.getInputStream());

        // initialize test peer and send message
        KademliaPeer mockedPeer = new KademliaPeer("127.0.0.1", 4000, 44);
        Message message = MsgUtils.createBase(mockedPeer)
                .setType(Message.MessageType.PING)
                .build();
        message.writeTo(bufferedOutputStream);
        bufferedOutputStream.flush();

        // receive message
        byte[] buffer = new byte[512];
        int read = bufferedInputStream.read(buffer);
        ByteBuffer messageBuffer = ByteBuffer.wrap(buffer, 0, read);
        Message receivedMessage = Message.parseFrom(messageBuffer);
        System.out.println(receivedMessage.toString());

        assertEquals(Message.MessageType.RESPONSE, receivedMessage.getType());
        assertEquals(KADEMLIA_SERVER_UUID, receivedMessage.getUuid());
        assertEquals(KADEMLIA_SERVER_HOST, receivedMessage.getSender().split(":")[0]);
        assertEquals(KADEMLIA_SERVER_PORT, Integer.parseInt(receivedMessage.getSender().split(":")[1]));

        assertEquals(1, kademliaNode.getPeers().size());
        assertEquals(44, kademliaNode.getPeers().get(0).getId());
        assertEquals("127.0.0.1", kademliaNode.getPeers().get(0).getAddress());
        assertEquals(4000, kademliaNode.getPeers().get(0).getPort());
    }

    private KademliaNode setupKademliaNode() {
        MessageHandler messageHandler = new MessageHandler();
        MessageReceiver messageReceiver = new MessageReceiver(messageHandler);
        NodeManager nodeManager = new NodeManager();
        KademliaNode kademliaNode = null;
        try {
            kademliaNode = new KademliaNode(
                    new Server(KADEMLIA_SERVER_PORT, messageReceiver, nodeManager),
                    new Client(null, messageReceiver, nodeManager),
                    KADEMLIA_SERVER_UUID,
                    MAX_BUCKET_SIZE,
                    null,
                    messageHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return kademliaNode;
    }

    @Test
    public void findNodeAndFoundNodesTest() throws IOException, InterruptedException {
        KademliaNode kademliaNode = setupKademliaNode();

        // socket that sends foundNodes Message
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(KADEMLIA_SERVER_HOST, KADEMLIA_SERVER_PORT));
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(socket.getOutputStream());

        // message
        Message foundNodes = MsgUtils.createFoundNodes(
                new KademliaPeer("127.0.0.1", 4000, 44),
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

        Thread.sleep(1000);

        assertEquals(7, kademliaNode.getPeers().size());
        Assert.assertThat(kademliaNode.getPeers(), hasItems(
                new KademliaPeer("127.11.11.1", 10, 0b01010101),
                new KademliaPeer("127.11.11.12", 10, 0b00110101),
                new KademliaPeer("127.11.11.13", 10, 0b00100101),
                new KademliaPeer("127.11.11.14", 10, 0b01010111),
                new KademliaPeer("127.11.11.15", 10, 0b10110101),
                new KademliaPeer("127.11.11.16", 10, 0b00111101),
                new KademliaPeer("127.11.11.17", 10, 0b00011101)
        ));


        // send findNode request
        socket.close();
        KademliaPeer mockPeer = new KademliaPeer("127.0.0.1", 4000, 0b00010101);
        socket = new Socket();
        socket.connect(new InetSocketAddress(KADEMLIA_SERVER_HOST, KADEMLIA_SERVER_PORT));
        bufferedOutputStream = new BufferedOutputStream(socket.getOutputStream());
        BufferedInputStream bufferedInputStream = new BufferedInputStream(socket.getInputStream());
        Message findNode = MsgUtils
                .createFindNode(mockPeer);
        findNode.writeTo(bufferedOutputStream);
        bufferedOutputStream.flush();


        // receive message
        byte[] buffer = new byte[512];
        int read = bufferedInputStream.read(buffer);
        ByteBuffer messageBuffer = ByteBuffer.wrap(buffer, 0, read);
        Message receivedMessage = Message.parseFrom(messageBuffer);

        List<KademliaPeer> peers =
                MsgUtils.getNodeDescriptionsAsPeers(receivedMessage.getPFoundNodes().getNodesList());

        Assert.assertThat(peers, hasItems(
                new KademliaPeer("127.11.11.17", 10, 0b00011101),
                new KademliaPeer("127.11.11.16", 10, 0b00111101),
                new KademliaPeer("127.11.11.12", 10, 0b00110101),
                new KademliaPeer("127.11.11.13", 10, 0b00100101))
        );
    }
}