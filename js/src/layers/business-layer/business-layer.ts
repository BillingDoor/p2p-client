import { prop } from 'ramda';
import { first, tap, map, filter, mergeAll } from 'rxjs/operators';

import { Address, Contact } from '@models';
import { P2PLayer } from '@layers/p2p-layer/p2p-layer';
import { Message } from '../../protobuf/Message_pb';

export class BusinessLayer {
  private pingedNodes: Contact[] = [];

  constructor(private worker: P2PLayer, private me: Contact) {}

  joinNetwork(node: Address) {
    this.worker.findNode({
      node,
      guid: this.me.guid
    });

    const nodes$ = this.worker
      .on(Message.MessageType.FOUND_NODES)
      .pipe(
        first(),
        map(prop('data')),
        map((x) => x.getPfoundnodes()),
        filter(Boolean),
        map((x) => x.getNodesList())
      );

    nodes$.pipe(
      mergeAll(),
      tap((node: Contact) => this.worker.routingTable.addNode(node)),
      tap((node) => this.worker.ping(node)),
      tap((node) => this.pingedNodes.push(node))
    );

    return nodes$;
  }
}
