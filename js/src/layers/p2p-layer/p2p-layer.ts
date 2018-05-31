import { Observable } from 'rxjs';

import { Address, Communication, Contact } from '@models';
import { MessageLayer } from '@layers//message-layer/message-layer';
import { Message } from '../../protobuf/Message_pb';
import * as utils from '@protobuf/utils';

import { RoutingTable } from './routing-table/routing-table';

export class P2PLayer {
  routingTable: RoutingTable;

  constructor(private worker: MessageLayer, private me: Contact) {
    this.routingTable = new RoutingTable(this.me);
  }

  findNode(config: { to: Address; guid: string }) {
    const { to, guid } = config;
    this.worker.send({
      data: utils.prepareFindNodeMessage({
        node: guid,
        sender: this.me
      }),
      address: to
    });
  }

  foundNodes(config: { to: Address; nodes: Message.Contact[] }) {
    const { to, nodes } = config;
    this.worker.send({
      data: utils.prepareFoundNodesMessage({
        nodes,
        sender: this.me
      }),
      address: to
    });
  }

  ping(node: Contact) {
    this.worker.send({
      data: utils.prepareBaseMessage({
        sender: this.me,
        type: Message.MessageType.PING
      }),
      address: node.address
    });
  }

  on(type: Message.MessageType): Observable<Communication<Message>> {
    console.log(`on(${type})`);
    return this.worker.on(type);
  }
}
