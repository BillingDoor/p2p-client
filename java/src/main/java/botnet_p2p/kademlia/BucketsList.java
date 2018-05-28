package botnet_p2p.kademlia;

import java.util.*;
import java.util.stream.Collectors;

public class BucketsList {
    private final int maxBucketSize;
    private int k;
    private ArrayList<ArrayList<KademliaPeer>> buckets;
    private long selfId;

    public BucketsList(int idBits, int maxBucketSize, int selfId) {
        this.buckets = new ArrayList<>(idBits);
        this.buckets.addAll(Collections.nCopies(idBits, new ArrayList<>()));
        this.maxBucketSize = maxBucketSize;
        this.selfId = selfId;
    }

    public BucketsList(int selfId) {
        this(128, 20, selfId);
    }


    public synchronized List<KademliaPeer> getNearestPeers(long id, int limit) {
        List<KademliaPeer> collected = this.buckets
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
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

        long distance = kademliaPeer.getId() ^ selfId;
        int index = largestDifferingBit(distance);


        ArrayList<KademliaPeer> bucket = buckets.get(index);
        if (bucket.size() >= this.maxBucketSize) {
            buckets.remove(0);
        }
        if (!bucket.contains(kademliaPeer)) {
            bucket.add(kademliaPeer);
        }
    }

    private int largestDifferingBit(long distance) {
        int length = -1;
        while (distance > 0) {
            distance >>= 1;
            length++;
        }
        return Math.max(0, length);
    }


}
