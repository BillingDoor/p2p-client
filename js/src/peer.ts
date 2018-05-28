import * as net from 'net';
import { reject, equals } from 'ramda';

import {
  encodeMessage,
  decodeMessage,
  prepareFindNodeMessage
} from './protobuf-utils';

import { Contact } from './contact/contact';
import { RoutingTable } from './routing-table/routing-table';
import { Message } from '../protobuf/Message_pb';

interface ServerConfig {
  server: net.Server;
  clients: net.Socket[];
}

export class Peer {
  contact: Contact;
  routingTable: RoutingTable;
  serverConfig: ServerConfig;

  constructor(config: { host: string; port: number }) {
    this.contact = new Contact(config);
    this.routingTable = new RoutingTable(this.contact);
    this.serverConfig = {} as ServerConfig;
    this.createServer();
  }

  createServer(): void {
    let clients = [] as net.Socket[];
    const server = net.createServer((socket) => {
      clients = [...this.serverConfig.clients, socket];

      socket.on('data', (data: Buffer) => {
        const msg = decodeMessage(data);
        if (msg.getType() == Message.MessageType.FIND_NODE) {
          this.routingTable.addNode(
            new Contact({
              host: socket.remoteAddress as string,
              port: socket.remotePort as number,
              guid: (msg.getPfindnode() as Message.FindNode).getGuid()
            })
          );
          return;
        }
      });
      // TODO: handle msg and return response
      socket.pipe(socket);

      socket.on('end', (data: any) => {
        clients = reject(equals(socket), clients);
      });
    });

    this.serverConfig = {
      server,
      clients
    };
  }

  runServer(): void {
    this.serverConfig.server.listen(this.contact.port, this.contact.host);
  }

  stopServer(): void {
    console.log('Stopping server');
    this.serverConfig.server.close();
  }

  connectTo(config: { host: string; port: number }) {
    const { host, port } = config;
    const msg = prepareFindNodeMessage({
      sender: this.contact.guid,
      target: this.contact.guid,
      host,
      port
    });

    var client = new net.Socket();
    client.connect(port, host, function() {
      console.log('Connected');
      client.write(encodeMessage(msg));
    });

    client.on('data', function(data) {
      console.log('Received: ' + decodeMessage(data));
      client.destroy(); // kill client after server's response
    });

    client.on('close', function() {
      console.log('Connection closed');
    });
  }

  joinNetwork(config: { host: string; port: number }): void {}
}
