import * as net from 'net';
import { Subject, Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

import { Address, Communication } from '@models';
import { compose, equals, nth, reject } from 'ramda';

export class SocketLayer {
  private server: net.Server = {} as net.Server;
  private connections: [Address, net.Socket][] = [];
  private receivedMessages$: Subject<Buffer>;

  constructor(private port: number) {
    this.receivedMessages$ = new Subject();
    this.handleReceivedMessages();
  }

  close(): void {
    console.log('Socket layer: closing connections.');
    this.receivedMessages$.complete();
    this.server.close();
    this.connections.forEach(([address, connection]) => connection.destroy());
  }

  getReceivedMessagesStream(): Observable<Buffer> {
    return this.receivedMessages$.asObservable();
  }

  setMessagesToSendStream(messagesToSend$: Observable<Communication<Buffer>>) {
    messagesToSend$.pipe(tap((msg) => this.send(msg))).subscribe();
  }

  private send(config: Communication<Buffer>): void {
    const { data, address } = config;
    const { host, port } = address;

    let client: net.Socket;

    const connected = this.connections.find(compose(equals(address), nth(1)));
    if (connected) {
      [, client] = connected;
      client.write(data);
    } else {
      client = new net.Socket();
      console.log(`Socket layer: Connecting to ${host}:${port}...`);
      client.connect(port, host);

      client.on('connect', () => {
        console.log(`Socket layer: Connected to ${host}:${port}!`);
        this.connections = [...this.connections, [address, client]];
        client.write(data);
      });

      client.on('close', () => {
        this.connections = reject(
          compose(equals(address), nth(1)),
          this.connections
        );
      });
    }
  }

  private handleReceivedMessages(): void {
    console.log(`Socket layer: Listening on port:${this.port}`);
    this.server = net.createServer((socket) => {
      socket.on('data', (data: Buffer) => {
        console.log('Socket layer: new message!');
        this.receivedMessages$.next(data);
      });
    });
    this.server.listen(this.port);
  }
}
