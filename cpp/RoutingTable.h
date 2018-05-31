#ifndef CPP_ROUTINGTABLE_H
#define CPP_ROUTINGTABLE_H


#include <cstdint>
#include <netinet/in.h>
#include <mutex>
#include <queue>

class RoutingTable {

 public:
  class Entry {
   public:
    in_addr_t host;
    uint16_t port;
    uint64_t id;
  };

//  std::array<std::queue<10, Entry>>, buckets_number> buckets;
  std::mutex mutex;

  Entry self;

  void insert(Entry);
  uint64_t *nearest_nodes();

  static const int buckets_number = 64;

  static uint64_t largest_differing_bit(uint64_t a, uint64_t b) {
    uint64_t distance = a ^b;
    uint64_t length = 0;

    while(distance) {
      distance >>= 1;
      ++length;
    }
    return length;
  }
};


#endif //CPP_ROUTINGTABLE_H
