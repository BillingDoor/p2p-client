import * as net from 'net';
import { compose, equals, nth, reject } from 'ramda';
import { Subject, Observable } from 'rxjs';
import { tap, finalize } from 'rxjs/operators';

import { Address, Communication } from '@models';
import logger from '@utils/logging';

export class SocketLayer {
  private server: net.Server = {} as net.Server;
  private connections: [Address, net.Socket][] = [];
  private receivedMessages$: Subject<Buffer>;

  constructor(private port: number) {
    this.receivedMessages$ = new Subject();
    this.handleReceivedMessages();
  }

  close(): void {
    logger.info('Socket layer: closing connections.');
    this.server.close();
    this.receivedMessages$.complete();
  }

  getReceivedMessagesStream(): Observable<Buffer> {
    return this.receivedMessages$.asObservable();
  }

  setMessagesToSendStream(messagesToSend$: Observable<Communication<Buffer>>) {
    messagesToSend$
      .pipe(
        tap((msg) => this.send(msg)),
        finalize(() =>
          this.connections.forEach(([address, connection]) =>
            connection.destroy()
          )
        )
      )
      .subscribe();
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
      logger.info(`Socket layer: Connecting to ${host}:${port}...`);
      client.connect(port, host);

      client.on('connect', () => {
        logger.info(`Socket layer: Connected to ${host}:${port}!`);
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
    logger.info(`Socket layer: Listening on port:${this.port}`);
    this.server = net.createServer((socket) => {
      socket.on('data', (data: Buffer) => {
        logger.info('Socket layer: new message!');
        this.receivedMessages$.next(data);
      });
    });
    this.server.listen(this.port);
  }
}
