import { Observable } from 'rxjs';

import { Contact } from '@models';
import { MessageLayer } from '@layers//message-layer/message-layer';
import { Message } from '../../protobuf/Message_pb';
import * as utils from '@protobuf/utils';

import { RoutingTable } from './routing-table/routing-table';

export class P2PLayer {
  routingTable: RoutingTable;

  constructor(private worker: MessageLayer, private me: Contact) {
    this.routingTable = new RoutingTable(this.me);
  }

  findNode(config: { to: Contact; guid: string }) {
    const { to, guid } = config;
    this.worker.send(
      utils.prepareFindNodeMessage({
        node: guid,
        sender: this.me,
        receiver: to
      })
    );
  }

  foundNodes(config: { to: Contact; nodes: Message.Contact[] }) {
    const { to, nodes } = config;
    this.worker.send(
      utils.prepareFoundNodesMessage({
        nodes,
        sender: this.me,
        receiver: to
      })
    );
  }

  ping(node: Contact) {
    this.worker.send(
      utils.prepareBaseMessage({
        type: Message.MessageType.PING,
        sender: this.me,
        receiver: node
      })
    );
  }

  on(type: Message.MessageType): Observable<Message> {
    console.log(`on(${type})`);
    return this.worker.on(type);
  }
}
