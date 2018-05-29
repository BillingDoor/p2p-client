package botnet_p2p.kademlia;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.hasItems;

public class BucketsListTest {
    @Test
    public void testInsert() {
        BucketsList bucketsList = new BucketsList(8, 2, 30);

        KademliaPeer kademliaPeer = new KademliaPeer("127.0.0.1", 80, 26);
        bucketsList.insert(kademliaPeer);
        Assert.assertEquals(kademliaPeer, bucketsList.getPeerById(26));
    }


    @Test
    public void insertedItemShouldBeAdded() {
        BucketsList bucketsList = new BucketsList(15, 10, 0);
        KademliaPeer addedPeer = new KademliaPeer("127.11.11.1", 10, 0b01010101);
        bucketsList.insert(addedPeer);
        bucketsList.insert(new KademliaPeer("194.4.4.4", 10, 0b00110101));
        bucketsList.insert(new KademliaPeer("127.11.11.1", 10, 0b00100101));

        KademliaPeer foundPeer = bucketsList.getPeerById(0b01010101);
        Assert.assertEquals(addedPeer, foundPeer);
        Assert.assertNull(bucketsList.getPeerById(0b01110101));
    }

    @Test
    public void testSize() {
        BucketsList bucketsList = new BucketsList(15, 10, 0);

        bucketsList.insert(new KademliaPeer("127.11.11.1", 10, 0b01010101));
        Assert.assertEquals(1, bucketsList.size());
        bucketsList.insert(new KademliaPeer("127.11.11.12", 10, 0b00110101));
        bucketsList.insert(new KademliaPeer("127.11.11.13", 10, 0b00100101));
        bucketsList.insert(new KademliaPeer("127.11.11.14", 10, 0b01010111));
        bucketsList.insert(new KademliaPeer("127.11.11.15", 10, 0b10110101));
        bucketsList.insert(new KademliaPeer("127.11.11.16", 10, 0b00111101));
        Assert.assertEquals(6, bucketsList.size());

        bucketsList.insert(new KademliaPeer("127.11.11.17", 10, 0b00011101));
        Assert.assertEquals(7, bucketsList.size());
    }

    @Test
    public void testNearest() {
        BucketsList bucketsList = new BucketsList(15, 10, 0);

        KademliaPeer keyPeer = new KademliaPeer("127.11.11.1", 10, 0b00010101);

        bucketsList.insert(keyPeer);
        bucketsList.insert(new KademliaPeer("127.11.11.1", 10, 0b01010101));
        bucketsList.insert(new KademliaPeer("127.11.11.12", 10, 0b00110101));
        bucketsList.insert(new KademliaPeer("127.11.11.13", 10, 0b00100101));
        bucketsList.insert(new KademliaPeer("127.11.11.14", 10, 0b01010111));
        bucketsList.insert(new KademliaPeer("127.11.11.15", 10, 0b10110101));
        bucketsList.insert(new KademliaPeer("127.11.11.16", 10, 0b00111101));
        bucketsList.insert(new KademliaPeer("127.11.11.17", 10, 0b00011101));

        List<KademliaPeer> nearest = bucketsList.getNearestPeers(keyPeer.getId(), 1);
        Assert.assertEquals(nearest.get(0), keyPeer);


        nearest = bucketsList.getNearestPeers(keyPeer.getId(), 4);
        Assert.assertEquals(4, nearest.size());
        Assert.assertThat(nearest, hasItems(
                new KademliaPeer("127.11.11.17", 10, 0b00011101),
                new KademliaPeer("127.11.11.16", 10, 0b00111101),
                new KademliaPeer("127.11.11.12", 10, 0b00110101),
                keyPeer));
    }
}