package botnet.p2p_layer;


import botnet.model.KademliaPeer;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

public class BucketsList {
    private final int maxBucketSize;
    private ArrayList<ArrayList<KademliaPeer>> buckets;
    private String myGuid;

    public BucketsList(int idBits, int maxBucketSize, String myGuid) {
        this.buckets = new ArrayList<>(idBits);
        for (int i = 0; i < idBits; i++) {
            this.buckets.add(new ArrayList<>(maxBucketSize));
        }
        this.maxBucketSize = maxBucketSize;
        this.myGuid = myGuid;
    }

    public BucketsList(String myGuid) {
        this(128, 20, myGuid);
    }

    public int largestDifferingBit(String guid1, String guid2) {
        BigInteger distance = xorGuids(guid1, guid2);
        return distance.bitLength() - 1;
    }

    public synchronized List<KademliaPeer> getNearestPeers(String id, int limit) {
        List<KademliaPeer> collected = getPeers();
        PriorityQueue<KademliaPeer> heap = new PriorityQueue<>(collected.size(), (o1, o2) -> (
                xorGuids(o1.getId(), id).compareTo(xorGuids(o2.getId(), id))
        ));
        heap.addAll(collected);

        List<KademliaPeer> foundNodes = new ArrayList<>();
        int i = 0;
        while (i < limit && !heap.isEmpty()) {
            foundNodes.add(heap.poll());
            i++;
        }
        return foundNodes;
    }

    private BigInteger xorGuids(String guid1, String guid2) {
        BigInteger g1 = new BigInteger(guid1);
        BigInteger g2 = new BigInteger(guid2);
        return g1.xor(g2);
    }

    public List<KademliaPeer> getPeers() {
        return this.buckets
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public synchronized KademliaPeer getPeerById(String guid) {
        for (ArrayList<KademliaPeer> bucket : buckets) {
            for (KademliaPeer kademliaPeer : bucket) {
                if (kademliaPeer.getId().equals(guid)) {
                    return kademliaPeer;
                }
            }
        }
        return null;
    }

    public synchronized void insert(KademliaPeer kademliaPeer) {
        if (kademliaPeer.getId().equals(myGuid)) {
            return;
        }

        int index = largestDifferingBit(kademliaPeer.getId(), myGuid);

        ArrayList<KademliaPeer> bucket = buckets.get(index);
        if (bucket.size() >= this.maxBucketSize) {
            buckets.remove(0);
        }
        if (!bucket.contains(kademliaPeer)) {
            bucket.add(kademliaPeer);
        }
    }

    public synchronized int size() {
        return this.buckets.stream().map(ArrayList::size).reduce(0, (a, b) -> a + b);
    }

    public int getMaxBucketSize() {
        return maxBucketSize;
    }

    public int getBucketsCount() {
        return this.buckets.size();
    }
}
