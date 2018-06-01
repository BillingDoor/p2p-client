import { throwError } from 'rxjs';
import { first, tap, map, filter } from 'rxjs/operators';

import { Address, Contact } from '@models';
import { P2PLayer } from '@layers/p2p-layer/p2p-layer';
import { Message } from '@protobuf/Message_pb';

export class BusinessLayer {
  private pingedNodes: Contact[] = [];

  constructor(private worker: P2PLayer, private me: Contact) {
    this.worker.on(Message.MessageType.FIND_NODE).subscribe((msg) => {
      const sender = msg.getSender();
      const node = msg.getFindnode();
      if (sender && node) {
        this.worker.foundNodes({
          to: Contact.from(sender),
          nodes: this.worker.routingTable.getNearestNodes(node.getGuid())
        });
      } else {
        throw new Error('Business layer: Message sender not set.');
      }
    });

    this.worker.on(Message.MessageType.PING).pipe();
  }

  joinNetwork(bootstrapNode: Address) {
    this.worker.findNode({
      to: bootstrapNode,
      guid: this.me.guid
    });

    this.worker
      .on(Message.MessageType.FOUND_NODES)
      .pipe(
        first(),
        this.addNodeToRoutingTable(bootstrapNode),
        map((x) => x.getFoundnodes()),
        filter(Boolean),
        map((x) => x.getNodesList()),
        this.pingNodes()
      )
      .subscribe();

    return true;
  }

  private addNodeToRoutingTable(node: Address) {
    return tap((msg: Message) => {
      const sender = msg.getSender();
      if (sender) {
        this.worker.routingTable.addNode(
          new Contact({
            address: node,
            guid: sender.getGuid()
          })
        );
      } else {
        throwError('Business layer: Message sender not set.');
      }
    });
  }

  private pingNodes() {
    return tap((nodes: Message.Contact[]) => {
      nodes.map(Contact.from).forEach((node) => {
        console.log(`Business layer: Pinging node: ${node.guid}`);
        this.worker.ping(node);
        this.pingedNodes = [...this.pingedNodes, node];
      });
    });
  }
}
