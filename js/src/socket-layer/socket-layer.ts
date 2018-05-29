import * as net from 'net';
import { reject, equals } from 'ramda';
import { Subject } from 'rxjs';

export class SocketLayer {
  server: net.Server = {} as net.Server;
  clients: net.Socket[] = [];

  constructor(private port: number, private messages$: Subject<Buffer>) {
    this.create();
  }

  private create(): void {
    this.server = net.createServer((socket) => {
      this.clients = [...this.clients, socket];

      socket.on('data', (data: Buffer) => {
        this.messages$.next(data);
      });

      socket.on('end', (data: Buffer) => {
        this.clients = reject(equals(socket), this.clients);
        if (this.clients.length == 0) {
          this.messages$.complete();
        }
      });
    });
  }

  listen(): void {
    this.server.listen(this.port);
  }

  stopListening(): void {
    console.log('Stopping server');
    this.server.close();
  }

  connectTo(config: { host: string; port: number }) {
    const { host, port } = config;

    const client = new net.Socket();
    client.connect(port, host, () => {
      console.log('Connected');
      client.write('xxxd');
    });

    client.on('data', function(data) {
      console.log('Received: ' + data);
      client.destroy(); // kill client after server's response
    });

    client.on('close', function() {
      console.log('Connection closed');
    });
    // return client;
  }
}
