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
    logger.info('P2P layer: closing.');
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

  fileChunk(config: {
    to: Contact;
    uuid: string;
    fileName: string;
    fileSize: number;
    ordinal: number;
    data: Buffer;
  }): Promise<void> {
    logger.info('P2P layer: Creating fileChunk message.');
    const { to, uuid, fileName, fileSize, ordinal, data } = config;
    return this.worker.send(
      utils.prepareFileChunkMessage({
        uuid,
        fileName,
        fileSize,
        ordinal,
        data,
        sender: this.me,
        receiver: to
      })
    );
  }

  fileRequest(config: { to: Address; path: string }): Promise<void> {
    logger.info('P2P layer: Creating fileRequest message.');
    const { to, path } = config;
    return this.worker.send(
      utils.prepareFileRequestMessage({
        path,
        sender: this.me,
        receiver: new Contact({ address: to })
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

  leave(config: { to: Contact }) {
    logger.info('P2P layer: Creating leave message.');
    const { to } = config;
    this.worker.send(
      utils.prepareBaseMessage({
        type: Message.MessageType.LEAVE,
        sender: this.me,
        receiver: to
      })
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
