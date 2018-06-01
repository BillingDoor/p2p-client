package botnet_p2p.p2p_layer;


import botnet_p2p.model.KademliaPeer;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

class BucketsList {
    private final int maxBucketSize;
    private ArrayList<ArrayList<KademliaPeer>> buckets;
    private String myGuid;

    BucketsList(int idBits, int maxBucketSize, String myGuid) {
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

    int largestDifferingBit(String guid1, String guid2) {
        BigInteger distance = xorGuids(guid1, guid2);
        return distance.bitLength() - 1;
    }

    synchronized List<KademliaPeer> getNearestPeers(String id) {
        List<KademliaPeer> collected = getPeers();
        List<KademliaPeer> foundNodes = new ArrayList<>();
        if(collected.size() == 0) {
            return foundNodes;
        }
        PriorityQueue<KademliaPeer> heap = new PriorityQueue<>(collected.size(), (o1, o2) -> (
                xorGuids(o1.getGuid(), id).compareTo(xorGuids(o2.getGuid(), id))
        ));
        heap.addAll(collected);

        int i = 0;
        while (i < this.maxBucketSize && !heap.isEmpty()) {
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

    private synchronized List<KademliaPeer> getPeers() {
        return this.buckets
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    synchronized KademliaPeer getPeerById(String guid) {
        for (ArrayList<KademliaPeer> bucket : buckets) {
            for (KademliaPeer kademliaPeer : bucket) {
                if (kademliaPeer.getId().equals(guid)) {
                    return kademliaPeer;
                }
            }
        }
        return null;
    }

    synchronized void insert(KademliaPeer kademliaPeer) {
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

    synchronized int size() {
        return this.buckets.stream().map(ArrayList::size).reduce(0, (a, b) -> a + b);
    }

    int getMaxBucketSize() {
        return maxBucketSize;
    }

    int getBucketsCount() {
        return this.buckets.size();
    }
}
