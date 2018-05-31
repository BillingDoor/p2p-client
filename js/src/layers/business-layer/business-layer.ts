import { prop } from 'ramda';
import { first, tap, map, filter } from 'rxjs/operators';

import { Address, Contact } from '@models';
import { P2PLayer } from '@layers/p2p-layer/p2p-layer';
import { Message } from '../../protobuf/Message_pb';

export class BusinessLayer {
  // private pingedNodes: Contact[] = [];

  constructor(private worker: P2PLayer, private me: Contact) {
    this.worker
      .on(Message.MessageType.FIND_NODE)
      .pipe(
        // map(prop('data')),
        // map((x) => x.getFindnode()),
        filter(Boolean)
        // map((x) => x.getGuid())
      )
      .subscribe((com) => {
        this.worker.foundNodes({
          node: com.address,
          guid: 'bla'
        });
        console.log(
          'FIND_NODES',
          com.address,
          com.data.getFindnode().getGuid()
        );
      });
  }

  joinNetwork(node: Address) {
    console.log('.joinNetwork');
    this.worker.findNode({
      node,
      guid: this.me.guid
    });

    const nodes$ = this.worker
      .on(Message.MessageType.FOUND_NODES)
      .pipe(
        tap(()=>console.log('works')),
        first(),
        map(prop('data')),
        map((x) => x.getFoundnodes()),
        filter(Boolean),
        map((x) => x.getNodesList())
      );

    nodes$.subscribe();
      // .pipe(
      //   mergeAll(),
      //   tap((node: Contact) => this.worker.routingTable.addNode(node)),
      //   tap((node) => this.worker.ping(node)),
      //   tap((node) => this.pingedNodes.push(node))
      //   // tap(() => console.log('It all worked!'))
      // )
      // .subscribe();

    return nodes$;
  }
}
