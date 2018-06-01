import { Observable } from 'rxjs';

import { Address, Contact } from '@models';
import { MessageLayer } from '@layers//message-layer/message-layer';
import { Message } from '@protobuf/Message_pb';
import * as utils from '@protobuf/utils';

import { RoutingTable } from './routing-table/routing-table';

export class P2PLayer {
  routingTable: RoutingTable;

  constructor(private worker: MessageLayer, private me: Contact) {
    this.routingTable = new RoutingTable(this.me);
  }

  findNode(config: { to: Address; guid: string }) {
    const { to, guid } = config;
    console.log('P2P layer: Creating findNode message');
    this.worker.send(
      utils.prepareFindNodeMessage({
        node: guid,
        sender: this.me,
        receiver: new Contact({ address: to, guid: 'not_set' })
      })
    );
  }

  foundNodes(config: { to: Contact; nodes: Contact[] }) {
    const { to, nodes } = config;
    console.log('P2P layer: Creating foundNodes message');
    this.worker.send(
      utils.prepareFoundNodesMessage({
        nodes: nodes.map((node) => node.toMessageContact()),
        sender: this.me,
        receiver: to
      })
    );
  }

  ping(node: Contact) {
    console.log('P2P layer: Creating ping message');
    this.worker.send(
      utils.prepareBaseMessage({
        type: Message.MessageType.PING,
        sender: this.me,
        receiver: node
      })
    );
  }

  on(type: Message.MessageType): Observable<Message> {
    return this.worker.on(type);
  }
}
