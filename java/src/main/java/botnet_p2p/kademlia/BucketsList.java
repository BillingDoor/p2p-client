package botnet_p2p.kademlia;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

public class BucketsList {
    private final int maxBucketSize;
    private ArrayList<ArrayList<KademliaPeer>> buckets;
    private long selfId;

    public BucketsList(int idBits, int maxBucketSize, long selfId) {
        this.buckets = new ArrayList<>(idBits);
        for (int i = 0 ; i < idBits; i++) {
            this.buckets.add(new ArrayList<>(maxBucketSize));
        }
        this.maxBucketSize = maxBucketSize;
        this.selfId = selfId;
    }

    public BucketsList(int selfId) {
        this(128, 20, selfId);
    }


    public synchronized List<KademliaPeer> getNearestPeers(long id, int limit) {
        List<KademliaPeer> collected = getPeers();
        PriorityQueue<KademliaPeer> heap = new PriorityQueue<>(collected.size(), (o1, o2) -> (
                Long.compare((o1.getId() ^ id), (o2.getId() ^ id))));
        heap.addAll(collected);

        List<KademliaPeer> foundNodes = new ArrayList<>();
        int i = 0;
        while(i < limit && !heap.isEmpty() )
        {
            foundNodes.add(heap.poll());
            i++;
        }
        return foundNodes;
    }

    public List<KademliaPeer> getPeers() {
        return this.buckets
                    .stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
    }

    public synchronized KademliaPeer getPeerById(long guid) {
        for (ArrayList<KademliaPeer> bucket : buckets) {
            for (KademliaPeer kademliaPeer : bucket) {
                if (kademliaPeer.getId() == guid) {
                    return kademliaPeer;
                }
            }
        }
        return null;
    }

    public synchronized void insert(KademliaPeer kademliaPeer) {
        if (kademliaPeer.getId() == selfId) {
            return;
        }

        int index = largestDifferingBit(kademliaPeer.getId(), selfId);

        ArrayList<KademliaPeer> bucket = buckets.get(index);
        if (bucket.size() >= this.maxBucketSize) {
            buckets.remove(0);
        }
        if (!bucket.contains(kademliaPeer)) {
            bucket.add(kademliaPeer);
        }
    }

    public synchronized int size() {
        return this.buckets.stream().map(ArrayList::size).reduce(0,(a, b) -> a + b);
    }

    public static int largestDifferingBit(long value1, long value2) {
        long distance = value1 ^ value2;
        int length = -1;
        while (distance > 0) {
            distance >>= 1;
            length++;
        }
        return Math.max(0, length);
    }

    public int getMaxBucketSize() {
        return maxBucketSize;
    }

    public int getBucketsCount() {
        return this.buckets.size();
    }
}
