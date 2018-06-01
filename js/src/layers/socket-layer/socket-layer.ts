import * as net from 'net';
import { Subject, Observable } from 'rxjs';
import { takeUntil, tap } from 'rxjs/operators';

import { Communication } from '@models';

export class SocketLayer {
  private server: net.Server = {} as net.Server;

  constructor(
    private port: number,
    private receivedMessages$: Subject<Buffer>,
    private messagesToSend$: Observable<Communication<Buffer>>
  ) {
    this.handleMessagesToSend();
    this.listen();
  }

  send(config: Communication<Buffer>): void {
    const { data, address } = config;
    const { host, port } = address;

    const client = new net.Socket();
    console.log(`Socket layer: Connecting to ${host}:${port}...`);
    client.connect(port, host);

    client.on('connect', () => {
      console.log(`Socket layer: Connected to ${host}:${port}!`);
      client.end(data);
      client.destroy();
    });
  }

  close(): void {
    this.receivedMessages$.complete();
    console.log('Socket layer: closing connections.');
    this.server.close();
  }

  private handleMessagesToSend() {
    this.messagesToSend$.pipe(tap((msg) => this.send(msg))).subscribe();
  }

  private listen(): void {
    console.log(`Socket layer: Listening on port:${this.port}`);
    this.server = net.createServer((socket) => {
      socket.on('data', (data: Buffer) => {
        console.log('Socket layer: new message!');
        this.receivedMessages$.next(data);
        socket.destroy();
      });
    });
    this.server.listen(this.port);
  }
}
