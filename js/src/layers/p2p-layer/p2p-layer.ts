import { Observable } from 'rxjs';

import { Address, Contact } from '@models';
import { MessageLayer } from '@layers//message-layer/message-layer';
import { Message } from '@protobuf/Message_pb';
import * as utils from '@protobuf/utils';
import logger from '@utils/logging';

import { RoutingTable } from './routing-table/routing-table';

export class P2PLayer {
  readonly routingTable: RoutingTable;

  constructor(private worker: MessageLayer, private me: Contact) {
    this.routingTable = new RoutingTable(this.me);
  }

  on(type: Message.MessageType): Observable<Message> {
    return this.worker.on(type);
  }

  close() {
    logger.info("P2P layer: closing.");    
    this.worker.close();
  }

  command(config: {
    to: Contact;
    command: string;
    shouldRespond?: boolean;
  }): Promise<void> {
    logger.info('P2P layer: Creating command message.');
    const { to, command, shouldRespond = false } = config;
    return this.worker.send(
      utils.prepareCommandMessage({
        command,
        shouldRespond,
        sender: this.me,
        receiver: to
      })
    );
  }

  commandResponse(config: {
    to: Contact;
    value: string;
    status: Message.Status;
    command: string;
  }): Promise<void> {
    logger.info('P2P layer: Creating command response message.');
    const { to, value, status, command } = config;
    return this.worker.send(
      utils.prepareCommandResponseMessage({
        value,
        status,
        command,
        sender: this.me,
        receiver: to
      })
    );
  }

  findNode(config: { to: Address; guid?: string }): Promise<void> {
    logger.info('P2P layer: Creating findNode message.');
    const { to, guid = this.me.guid } = config;
    return this.worker.send(
      utils.prepareFindNodeMessage({
        node: guid,
        sender: this.me,
        receiver: new Contact({ address: to })
      })
    );
  }

  foundNodes(config: { to: Contact; nodes: Contact[] }): Promise<void> {
    logger.info('P2P layer: Creating foundNodes message.');
    const { to, nodes } = config;
    return this.worker.send(
      utils.prepareFoundNodesMessage({
        nodes: nodes.map(Contact.toMessageContact),
        sender: this.me,
        receiver: to
      })
    );
  }

  leave() {
    logger.info('P2P layer: Creating leave messages.');
    this.routingTable.getAllNodes().forEach((node) =>
      this.worker.send(
        utils.prepareBaseMessage({
          type: Message.MessageType.LEAVE,
          sender: this.me,
          receiver: node
        })
      )
    );
  }

  ping(node: Contact): Promise<void> {
    logger.info('P2P layer: Creating ping message.');
    return this.worker.send(
      utils.prepareBaseMessage({
        type: Message.MessageType.PING,
        sender: this.me,
        receiver: node
      })
    );
  }

  pingResponse(config: { to: Contact }): Promise<void> {
    logger.info('P2P layer: Creating pingResponse message.');
    const { to } = config;
    return this.worker.send(
      utils.prepareBaseMessage({
        type: Message.MessageType.PING_RESPONSE,
        sender: this.me,
        receiver: to
      })
    );
  }
}
