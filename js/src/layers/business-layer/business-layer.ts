import { first, tap, filter, delay } from 'rxjs/operators';

import { Address, Contact } from '@models';
import { P2PLayer } from '@layers/p2p-layer/p2p-layer';
import { Message } from '@protobuf/Message_pb';

export class BusinessLayer {
  private pingedNodes: Contact[] = [];

  constructor(private worker: P2PLayer, private me: Contact) {
    this.handleFindNodeMessage();
    this.handlePingMessage();
    this.handlePingResponseMessage();
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
        this.addNodeToRoutingTable(),
        this.pingNodes(),
        delay(60000),
        tap(() => {
          this.pingedNodes = [];
        })
      )
      .subscribe();

    return true;
  }

  close() {
    this.worker.close();
  }

  private handleFindNodeMessage() {
    this.worker
      .on(Message.MessageType.FIND_NODE)
      .pipe(
        this.addNodeToRoutingTable(),
        tap((msg) => {
          const sender = checkSender(msg);
          const node = msg.getFindnode();
          if (node) {
            this.worker.foundNodes({
              to: Contact.from(sender),
              nodes: this.worker.routingTable.getNearestNodes(node.getGuid())
            });
          } else {
            throw new Error('Business layer: FindNode message not set.');
          }
        })
      )
      .subscribe();
  }

  private handlePingMessage() {
    this.worker
      .on(Message.MessageType.PING)
      .pipe(
        this.addNodeToRoutingTable(),
        tap((msg) => {
          const sender = checkSender(msg);
          this.worker.pingResponse({
            to: Contact.from(sender)
          });
        })
      )
      .subscribe();
  }

  private handlePingResponseMessage() {
    this.worker
      .on(Message.MessageType.PING_RESPONSE)
      .pipe(
        filter((msg) => {
          const sender = checkSender(msg);
          return this.pingedNodes.includes(Contact.from(sender));
        }),
        this.addNodeToRoutingTable()
      )
      .subscribe();
  }

  private addNodeToRoutingTable(node?: Address) {
    return tap((msg: Message) => {
      const sender = checkSender(msg);
      this.worker.routingTable.addNode(Contact.from(sender));
    });
  }

  private pingNodes() {
    return tap((msg: Message) => {
      const found = msg.getFoundnodes();
      const nodes = found ? found.getNodesList() : [];
      nodes.map(Contact.from).forEach((node) => {
        console.log(`Business layer: Pinging node: ${node.guid}`);
        this.worker.ping(node);
        this.pingedNodes = [...this.pingedNodes, node];
      });
    });
  }
}

function checkSender(msg: Message): Message.Contact {
  const sender = msg.getSender();
  if (!sender) {
    throw new Error('Business layer: Message sender not set.');
  } else {
    return sender;
  }
}
