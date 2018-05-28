import { flatten, slice, find, propEq } from 'ramda';
import * as bigInt from 'big-integer';

import { Contact } from '../contact/contact';

export class RoutingTable {
  private buckets: Contact[][];

  static readonly bucketCount = 64;
  static readonly bucketSize = 10;

  constructor(private selfNode: Contact) {
    this.buckets = Array(RoutingTable.bucketCount).fill([]);
  }

  addNode(node: Contact): void {
    let bucket = this.buckets[this.selectBucket(node.guid)];
    console.log('bucket', this.selectBucket(node.guid));
    const notSelf = node.guid !== this.selfNode.guid;
    const bucketNotFull = bucket.length < RoutingTable.bucketSize;

    if (notSelf && bucketNotFull) {
      bucket = [...bucket, node];
    }
  }

  getNodeByGUID(guid: number): Contact | undefined {
    return find(propEq('guid', guid), this.buckets[this.selectBucket(guid)]);
  }

  getNearestNodes(
    limit = RoutingTable.bucketCount * RoutingTable.bucketSize
  ): Contact[] {
    return slice(-limit, Infinity)(flatten(this.buckets));
  }

  private selectBucket(guid: number): number {
    const xor = bigInt(guid).xor(bigInt(this.selfNode.guid));
    console.log(xor)
    return xor.toArray(2).value.length;
  }
}
