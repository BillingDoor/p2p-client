import * as bigInt from 'big-integer';
import {
  slice,
  find,
  propEq,
  equals,
  reject,
  flatten,
  sort,
  pipe
} from 'ramda';

import { Contact } from '@models/contact';

export class RoutingTable {
  private buckets: Contact[][];

  static readonly bucketCount = 64;
  static readonly bucketSize = 10;

  constructor(private selfNode: Contact) {
    this.buckets = Array(RoutingTable.bucketCount).fill([]);
  }

  addNode(node: Contact): void {
    let bucket = this.buckets[this.selectBucket(node.guid)];

    const notSelf = node.guid !== this.selfNode.guid;
    const bucketNotFull = bucket.length < RoutingTable.bucketSize;

    if (notSelf && bucketNotFull) {
      console.log(`P2P layer: Adding node: ${node.guid} to routing table`);
      bucket = [...bucket, node];
    }
  }

  removeNode(node: Contact): void {
    let bucket = this.buckets[this.selectBucket(node.guid)];
    bucket = reject(equals(node), bucket);
  }

  getNodeByGUID(guid: string): Contact | undefined {
    return find(propEq('guid', guid), this.buckets[this.selectBucket(guid)]);
  }

  getNearestNodes(guid: string, limit = RoutingTable.bucketSize): Contact[] {
    const nearestNodes = pipe<Contact[][], Contact[], Contact[], Contact[]>(
      flatten,
      sort((node) =>
        bigInt(node.guid)
          .xor(bigInt(guid))
          .compare(bigInt(guid))
      ),
      slice(0, limit)
    )(this.buckets);

    return nearestNodes;
  }

  private selectBucket(guid: string): number {
    const xor = bigInt(guid).xor(bigInt(this.selfNode.guid));
    return xor.bitLength().toJSNumber() - 1;
  }
}
