import heapq
import asyncio
import itertools
import logging.handlers

logging.basicConfig(
    level=logging.DEBUG,
    format='%(name)s: %(message)s',
)
handler = logging.handlers.RotatingFileHandler(
    "bl_log.txt",
    maxBytes=65536,
    backupCount=10
)
log = logging.getLogger(__name__)
log.addHandler(handler)

def largest_differing_bit(value1, value2):
    """
    Returns index(from 0 to 127) of largest differing bit: Eg. for argument 011010...0 and 011110...0 it returns 3.
    :param value1: First id
    :param value2: Second id
    :return: index(from 0 to 127) of largest differing bit.
    """
    distance = value1 ^ value2
    length = 0
    while (distance):
        distance >>= 1
        length += 1
    return length

class BucketList(object):
    """
    Data structure of BucketList. Basically it is list of list of size k.
    """
    def __init__(self, bucket_size, buckets_number, id):
        """
        :param bucket_size: Size of every bucket
        :param buckets_number: How many buckets to create
        """
        self.bucket_size = bucket_size
        self.buckets = [[] for i in range(buckets_number)]
        self.id = id
        self.lock = asyncio.Lock()

    def __len__(self):
        """
        Returns amount of nodes in bucketlist
        """
        return sum([len(bucket) for bucket in self.buckets])

    def __contains__(self, peer):
        """
        :return: True if peer is in one of the buckets, otherwise False
        """
        return peer in self.buckets[largest_differing_bit(self.id, peer.id)]

    async def get_peer_by_id(self, id):
        """
        Return Peer if in routing table. Otherwise None
        :param id: searched id
        :return: Found Peer or None
        """
        await self.lock.acquire()
        found_peer = None
        for peer in itertools.chain.from_iterable(self.buckets):
            if peer.id == id:
                log.debug("Found node {!r}".format(peer.get_info()))
                found_peer = peer

        self.lock.release()
        if found_peer is None:
            log.debug("Didn't find node of id {}".format(id))

        return found_peer

    async def insert(self, peer):
        """
        Insert peer into appropriate bucket
        :param peer: Peer to insert
        """
        if peer.id == self.id:
            return

        bucket_number = largest_differing_bit(self.id, peer.id)
        bucket = self.buckets[bucket_number]

        await self.lock.acquire()
        if len(bucket) >= self.bucket_size:
            log.debug("Bucket {!r} is full".format(bucket))
        if peer not in bucket:
            log.debug("Insert {!r} into bucket {!r}".format(peer.get_info(), bucket_number))
            bucket.append(peer)
        self.lock.release()

    async def nearest_nodes(self, key, limit=None):
        num_results = limit if limit else self.bucket_size

        await self.lock.acquire()
        peers = [peer for bucket in self.buckets for peer in bucket]
        self.lock.release()
        best_peers = heapq.nsmallest(num_results, peers, lambda p: key ^ int(p.get_info()[2]))
        return best_peers