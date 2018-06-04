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

  static readonly PREFIX_BYTES = 4;

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

    const prefixedData = prefixData(data);

    let client: net.Socket;

    const connected = this.connections.find(compose(equals(address), nth(1)));
    if (connected) {
      [, client] = connected;
      client.write(prefixedData);
    } else {
      client = new net.Socket();
      logger.info(`Socket layer: Connecting to ${host}:${port}...`);
      client.connect(port, host);

      client.on('connect', () => {
        logger.info(`Socket layer: Connected to ${host}:${port}!`);
        this.connections = [...this.connections, [address, client]];
        client.write(prefixedData);
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
      let messageLength: number;
      let message = Buffer.alloc(0);

      socket.on('data', (data: Buffer) => {
        message = Buffer.concat([message, data]);
        if (!messageLength && message.byteLength >= SocketLayer.PREFIX_BYTES) {
          messageLength = message.readUInt32BE(0);
        }
        if (message.byteLength >= messageLength + SocketLayer.PREFIX_BYTES) {
          logger.info('Socket layer: new message!');
          this.receivedMessages$.next(unPrefixData(message));
          messageLength = 0;
          message = Buffer.alloc(0);
        }
      });
    });
    this.server.listen(this.port);
  }
}

function prefixData(data: Buffer): Buffer {
  const dataPrefix = Buffer.alloc(SocketLayer.PREFIX_BYTES);
  dataPrefix.writeUInt32BE(data.byteLength, 0);
  return Buffer.concat([dataPrefix, data]);
}

function unPrefixData(data: Buffer): Buffer {
  return data.slice(4);
}
