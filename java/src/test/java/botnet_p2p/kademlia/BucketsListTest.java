package botnet_p2p.kademlia;

import org.junit.Assert;
import org.junit.Test;

public class BucketsListTest {
    @Test
    public void testInsert() {
        BucketsList bucketsList = new BucketsList(8,2, 30);

        KademliaPeer kademliaPeer = new KademliaPeer("127.0.0.1", 80, 26);
        bucketsList.insert(kademliaPeer);
        Assert.assertEquals(kademliaPeer, bucketsList.getPeerById(26));
    }
}