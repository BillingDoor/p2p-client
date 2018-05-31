import * as net from 'net';
import { reject, equals } from 'ramda';
import { Subject, Observable } from 'rxjs';
import { Communication } from '../../models';
import { takeUntil, tap } from 'rxjs/operators';

export class SocketLayer {
  server: net.Server = {} as net.Server;
  clients: net.Socket[] = [];

  private closed$ = new Subject<void>();

  constructor(
    private port: number,
    private messages$: Subject<Communication<Buffer>>,
    private messagesToSend$: Observable<Communication<Buffer>>
  ) {
    this.listen();
    this.messagesToSend$
      .pipe(takeUntil(this.closed$), tap((msg) => this.send(msg)))
      .subscribe();
  }

  private listen(): void {
    this.server = net.createServer((socket) => {
      this.clients = [...this.clients, socket];

      socket.on('data', (data: Buffer) => {
        if (socket.remoteAddress && socket.remotePort) {
          this.messages$.next({
            data,
            address: {
              host: socket.remoteAddress,
              port: socket.remotePort
            }
          });
        }
      });

      socket.on('end', (data: Buffer) => {
        this.clients = reject(equals(socket), this.clients);
      });
    });
    this.server.listen(this.port);
  }

  send(config: Communication<Buffer>): void {
    const { data, address } = config;
    const { host, port } = address;

    const clientToRespond = this.clients.find(
      (client) => client.remoteAddress == host && client.remotePort == port
    );
    if (clientToRespond) {
      clientToRespond.end(data);
    } else {
      const client = new net.Socket();
      client.connect(port, host);

      client.on('connect', () => {
        console.log('Connected');
        client.write(data);
      });

      client.on('data', (data) => {
        this.messages$.next({
          data,
          address: {
            host,
            port
          }
        });
        client.destroy();
      });

      client.on('close', function() {
        console.log('Connection closed');
      });
    }
  }

  close(): void {
    this.server.close();
    this.clients.forEach((sock) => sock.destroy());
    this.closed$.next();
    this.closed$.complete();
    this.messages$.complete();
  }
}
