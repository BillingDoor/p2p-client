import { Observable } from 'rxjs';

import { Address, Communication, Contact } from '@models';
import { MessageLayer } from '@layers//message-layer/message-layer';
import { Message } from '../../protobuf/Message_pb';

import { RoutingTable } from './routing-table/routing-table';
import { preparePingMessage, prepareFindNodeMessage, prepareFoundNodesMessage } from 'protobuf-utils';

export class P2PLayer {
  routingTable: RoutingTable;

  constructor(private worker: MessageLayer, private me: Contact) {
    this.routingTable = new RoutingTable(this.me);
  }

  findNode(config: { node: Address; guid: string }) {
    console.log('.findNode');
    const { node, guid } = config;
    this.worker.send({
      data: prepareFindNodeMessage({
        senderGUID: '1',
        targetGUID: guid,
        address: this.me.address
      }),
      address: node
    });
  }

  foundNodes(config: { node: Address; guid: string }) {
    console.log('.foundNodes');
    const { node, guid } = config;
    this.worker.send({
      data: prepareFoundNodesMessage({
        senderGUID: '1',
        targetGUID: guid,
        address: this.me.address
      }),
      address: node
    });
  }

  ping(node: Contact) {
    console.log('.ping');
    this.worker.send({
      data: preparePingMessage({
        senderGUID: '1',
        targetGUID: '2',
        address: this.me.address
      }),
      address: node.address
    });
  }

  on(type: Message.MessageType): Observable<Communication<Message>> {
    console.log(`on(${type})`);
    return this.worker.on(type);
  }
}
