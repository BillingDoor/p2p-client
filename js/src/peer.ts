import * as net from 'net';
import { reject, equals } from 'ramda';

import { Contact } from './contact/contact';
import { RoutingTable } from './routing-table/routing-table';

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
    console.log('Stopping server!')
    this.serverConfig.server.close();
  }
}
