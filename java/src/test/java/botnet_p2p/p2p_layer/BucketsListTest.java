package botnet_p2p.p2p_layer;

import botnet_p2p.model.KademliaPeer;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BucketsListTest {

    @Test
    public void testInsert() {
        BucketsList bucketsList = new BucketsList(8, 2, "30");

        KademliaPeer kademliaPeer = new KademliaPeer("127.0.0.1", 80, 26);
        bucketsList.insert(kademliaPeer);
        assertEquals(kademliaPeer, bucketsList.getPeerById("26"));
    }

    private String intToStr(int bin) {
        return Integer.toString(bin);
    }

    @Test
    public void testInsertedItemsShouldBeAdded() {
        BucketsList bucketsList = new BucketsList(15, 10, "0");
        KademliaPeer addedPeer = new KademliaPeer("127.11.11.1", 10, intToStr(0b01010101));
        bucketsList.insert(addedPeer);
        bucketsList.insert(new KademliaPeer("194.4.4.4", 10, intToStr(0b00110101)));
        bucketsList.insert(new KademliaPeer("127.11.11.1", 10, intToStr(0b00100101)));

        KademliaPeer foundPeer = bucketsList.getPeerById(intToStr(0b01010101));
        assertEquals(addedPeer, foundPeer);
        assertNull(bucketsList.getPeerById(intToStr(0b01110101)));
    }

    @Test
    public void testSize() {
        BucketsList bucketsList = new BucketsList(15, 10, "0");

        bucketsList.insert(new KademliaPeer("127.11.11.1", 10, intToStr(0b01010101)));
        assertEquals(1, bucketsList.size());
        bucketsList.insert(new KademliaPeer("127.11.11.12", 10, intToStr(0b00110101)));
        bucketsList.insert(new KademliaPeer("127.11.11.13", 10, intToStr(0b00100101)));
        bucketsList.insert(new KademliaPeer("127.11.11.14", 10, intToStr(0b01010111)));
        bucketsList.insert(new KademliaPeer("127.11.11.15", 10, intToStr(0b10110101)));
        bucketsList.insert(new KademliaPeer("127.11.11.16", 10, intToStr(0b00111101)));
        assertEquals(6, bucketsList.size());

        bucketsList.insert(new KademliaPeer("127.11.11.17", 10, intToStr(0b00011101)));
        assertEquals(7, bucketsList.size());
    }

    @Test
    public void testNearest() {
        BucketsList bucketsList = new BucketsList(15, 10, "0");

        KademliaPeer keyPeer = new KademliaPeer("127.11.11.1", 10, intToStr(0b00010101));

        bucketsList.insert(keyPeer);
        bucketsList.insert(new KademliaPeer("127.11.11.1", 10, intToStr(0b01010101)));
        bucketsList.insert(new KademliaPeer("127.11.11.12", 10, intToStr(0b00110101)));
        bucketsList.insert(new KademliaPeer("127.11.11.13", 10, intToStr(0b00100101)));
        bucketsList.insert(new KademliaPeer("127.11.11.14", 10, intToStr(0b01010111)));
        bucketsList.insert(new KademliaPeer("127.11.11.15", 10, intToStr(0b10110101)));
        bucketsList.insert(new KademliaPeer("127.11.11.16", 10, intToStr(0b00111101)));
        bucketsList.insert(new KademliaPeer("127.11.11.17", 10, intToStr(0b00011101)));

        List<KademliaPeer> nearest = bucketsList.getNearestPeers(keyPeer.getGuid(), 1);
        assertEquals(nearest.get(0), keyPeer);


        nearest = bucketsList.getNearestPeers(keyPeer.getGuid(), 4);
        assertEquals(4, nearest.size());
        assertThat(nearest).containsOnly(
                new KademliaPeer("127.11.11.17", 10, intToStr(0b00011101)),
                new KademliaPeer("127.11.11.16", 10, intToStr(0b00111101)),
                new KademliaPeer("127.11.11.12", 10, intToStr(0b00110101)),
                keyPeer);
    }

}