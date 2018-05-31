#include "RoutingTable.h"

void RoutingTable::insert(Entry entry) {
  uint64_t bucket_index = largest_differing_bit(self.id, entry.id);
//  auto bucket = buckets[bucket_index];

  mutex.lock();
//    bucket[0] = entry;

  mutex.unlock();
}