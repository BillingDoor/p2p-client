package botnet_p2p.kademlia;

import botnet_p2p.Client;
import botnet_p2p.MessageHandler;
import botnet_p2p.MessageOuterClass.Message;
import botnet_p2p.Server;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class KademliaNode implements MessageListener {
    private static final Logger logger = LogManager.getLogger(KademliaNode.class);

    private Server server;
    private Client client;
    private BucketsList routingTable;

    private KademliaPeer me;
    private List<KademliaPeer> askedNodes;
    private Semaphore askedNodesAccess = new Semaphore(1);


    public KademliaNode(Server server, Client client, long ownId, KademliaPeer bootPeer, MessageHandler messageHandler) throws IOException {
        this.server = server;
        this.client = client;
        this.me = new KademliaPeer("127.0.0.1", 3000, ownId);
        this.routingTable = new BucketsList(64, 20, me.getId());
        this.askedNodes = new ArrayList<>();

        messageHandler.setFoundNodesListener(this);

        this.server.start();
        this.client.start();
        //bootstrap(bootPeer);
    }

    private void bootstrap(KademliaPeer bootPeer) throws IOException {
        this.routingTable.insert(bootPeer);
        this.askedNodes.add(bootPeer);
        this.sendFindNode(bootPeer);
    }

    private void sendFindNode(KademliaPeer peer) throws IOException {
        Message message = Message.newBuilder()
                .setType(Message.MessageType.FIND_NODE)
                .setPFindNode(Message.FindNode.newBuilder().setGuid(this.me.getId()))
                .build();

        this.client.sendMessage(message, peer.getAddress(), peer.getPort());
    }


    @Override
    public void foundNodesMessageReceived(Message message) {
        Message.FoundNodes pFoundNodes = message.getPFoundNodes();
        pFoundNodes.getNodesList().forEach(nodeDescription -> {
            try {
                askedNodesAccess.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // if not in askedNodes
            if (askedNodes
                    .stream().noneMatch(kademliaPeer -> kademliaPeer.getId() == nodeDescription.getGuid())) {

                KademliaPeer kademliaPeer = new KademliaPeer(nodeDescription.getIP(), Integer.parseInt(nodeDescription.getPort()), nodeDescription.getGuid());
                // add to routing table
                this.routingTable.insert(kademliaPeer);

                // add to askedNodes
                this.askedNodes.add(kademliaPeer);

                // send FindNode message
                try {
                    this.sendFindNode(kademliaPeer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            askedNodesAccess.release();
        });
    }

    @Override
    public void pingMessageReceived(Message message, SocketChannel sender) {
        logger.info("ping received");

        Message newMessage = Messages.getBase(this.me)
                .setType(Message.MessageType.RESPONSE)
                .build();

        try {
            client.sendMessage(newMessage, sender);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
