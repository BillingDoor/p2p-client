package botnet_p2p.kademlia;

import botnet_p2p.Client;
import botnet_p2p.MessageHandler;
import botnet_p2p.Server;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import static botnet_p2p.MessageOuterClass.Message;
import static botnet_p2p.kademlia.BucketsList.largestDifferingBit;

public class KademliaNode implements MessageListener {
    private static final Logger logger = LogManager.getLogger(KademliaNode.class);

    private Server server;
    private Client client;
    private BucketsList routingTable;

    private KademliaPeer me;
    private List<KademliaPeer> askedNodes;
    private Semaphore askedNodesAccess = new Semaphore(1);


    public KademliaNode(Server server, Client client, long ownId, int maxBucketSize, KademliaPeer bootPeer, MessageHandler messageHandler) throws IOException {
        this.server = server;
        this.client = client;
        this.me = new KademliaPeer("127.0.0.1", 3000, ownId);
        this.routingTable = new BucketsList(64, maxBucketSize, me.getId());
        this.askedNodes = new ArrayList<>();

        messageHandler.setFoundNodesListener(this);

        this.server.start();
        this.client.start();
        if (bootPeer != null) {
            bootstrap(bootPeer);
        } else {
            logger.error("No bootstrapPeer, boostrap will not be run");
        }
    }

    private void bootstrap(KademliaPeer bootPeer) throws IOException {
        this.routingTable.insert(bootPeer);
        this.askedNodes.add(bootPeer);
        this.sendFindNode(bootPeer);


        //
        // wait for all messages
        //


        int bootstrapBucketIndex = largestDifferingBit(me.getId(), bootPeer.getId());

        final int bucketsCount = routingTable.getBucketsCount();
        for (int i = bootstrapBucketIndex + 1; i <= bucketsCount; i++) {
            long id = me.getId();
            int mask = 1 << (bucketsCount - i - 1);
            id ^= mask;

            lookupNode(id, 3);
        }
        /*
                bootstrap_bucket_index = largest_differing_bit(key, boot_peer.id)

        for i in range(bootstrap_bucket_index + 1, len(self.routing_table.buckets)):
            id = key
            # Change bit on i position (from the most significant bit, indexing from 0)
            mask = 1 << (len(self.routing_table.buckets) - i - 1)
            id ^= mask

            _ = self.lookup_node(id)
        */
    }

    /**
     * @param destId the node we want to reach
     * @param alpha  how many contacts to use
     */
    private KademliaPeer lookupNode(long destId, int alpha) {
        // check if present in routing table
        KademliaPeer peer = this.routingTable.getPeerById(destId);
        if (peer != null) {
            return peer;
        }

        int k = routingTable.getMaxBucketSize();
        routingTable.getNearestPeers(destId, k);

        // int smallestDistance =

        return peer;
    }

    private void sendFindNode(KademliaPeer peer) throws IOException {
        Message message = MsgUtils.createFindNode(me);


        this.client.sendMessage(message, peer.getAddress(), peer.getPort());
    }


    @Override
    public void foundNodesMessageReceived(Message message) {
        Message.FoundNodes pFoundNodes = message.getPFoundNodes();
        try {
            askedNodesAccess.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        pFoundNodes.getNodesList().forEach(nodeDescription -> {
            // if not in askedNodes
            if (askedNodes
                    .stream().noneMatch(kademliaPeer -> kademliaPeer.getId() == nodeDescription.getGuid())) {

                KademliaPeer kademliaPeer = new KademliaPeer(
                        nodeDescription.getIP(),
                        Integer.parseInt(nodeDescription.getPort()),
                        nodeDescription.getGuid());
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
        });
        askedNodesAccess.release();
    }


    @Override
    public void findNodeMessageReceived(Message message, SocketChannel sender) {
        // respond with nearest nodes list
        List<KademliaPeer> nearestPeers = this.routingTable.getNearestPeers(message.getPFindNode().getGuid(),
                this.routingTable.getMaxBucketSize());
        Message newMessage = MsgUtils.createFoundNodes(this.me, nearestPeers);
        try {
            client.sendMessage(newMessage, sender);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // add sender to our routing table
        KademliaPeer senderPeer = MsgUtils.getSenderAsPeer(message);
        this.routingTable.insert(senderPeer);
    }

    @Override
    public void pingMessageReceived(Message message, SocketChannel sender) {
        logger.info("ping received");
        KademliaPeer kademliaPeer = MsgUtils.getSenderAsPeer(message);
        routingTable.insert(kademliaPeer);

        Message newMessage = MsgUtils.createBase(this.me)
                .setType(Message.MessageType.RESPONSE)
                .build();

        try {
            client.sendMessage(newMessage, sender);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<KademliaPeer> getPeers() {
        return routingTable.getPeers();
    }


}
