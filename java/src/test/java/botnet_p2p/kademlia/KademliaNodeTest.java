package botnet_p2p.kademlia;

import botnet_p2p.*;
import botnet_p2p.MessageOuterClass.Message;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;


public class KademliaNodeTest {

    private static final String KADEMLIA_SERVER_HOST = "127.0.0.1";
    private static final int KADEMLIA_SERVER_PORT = 3000;
    private static final int KADEMLIA_SERVER_UUID = 22;

    @Test
    public void pingTest() throws InterruptedException, IOException {
        MessageHandler messageHandler = new MessageHandler();
        MessageReceiver messageReceiver = new MessageReceiver(messageHandler);
        NodeManager nodeManager = new NodeManager();
        KademliaNode kademliaNode = null;
        try {
            kademliaNode = new KademliaNode(
                    new Server(KADEMLIA_SERVER_PORT, messageReceiver, nodeManager),
                    new Client(null, messageReceiver, nodeManager),
                    KADEMLIA_SERVER_UUID,
                    null,
                    messageHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // socket that sends ping message
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(KADEMLIA_SERVER_HOST, KADEMLIA_SERVER_PORT));
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(socket.getOutputStream());
        BufferedInputStream bufferedInputStream = new BufferedInputStream(socket.getInputStream());

        // initialize test peer and send message
        KademliaPeer mockedPeer = new KademliaPeer("127.0.0.1", 4000, 44);
        Message message = MsgUtils.getBase(mockedPeer)
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
}