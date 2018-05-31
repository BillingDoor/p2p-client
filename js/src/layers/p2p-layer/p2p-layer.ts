import { Observable } from 'rxjs';

import { Address, Communication, Contact } from '@models';
import { MessageLayer } from '@layers//message-layer/message-layer';
import { Message } from '../../protobuf/Message_pb';

import { RoutingTable } from './routing-table/routing-table';
import { preparePingMessage, prepareFindNodeMessage } from 'protobuf-utils';

export class P2PLayer {
  routingTable: RoutingTable;

  constructor(private worker: MessageLayer, private me: Contact) {
    this.routingTable = new RoutingTable(this.me);
  }

  findNode(config: { node: Address; guid: string }) {
    this.worker.send({
      data: prepareFindNodeMessage({
        
      })
    })
  }
  ping(node: Contact) {
    this.worker.send({
      data: preparePingMessage({
        sender: '1',
        target: '2',
        host: 'localhost',
        port: 2345
      }),
      address: {
        host: node.host,
        port: node.port
      }
    });
  }

  on(type: Message.MessageType): Observable<Communication<Message>> {
    return this.worker.on(type);
  }
}
