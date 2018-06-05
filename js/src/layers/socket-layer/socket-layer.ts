import * as net from 'net';
import { compose, equals, nth, reject as ramdaReject, find } from 'ramda';
import { Subject, Observable } from 'rxjs';

import { Address, Communication } from '@models';
import logger from '@utils/logging';

export class SocketLayer {
  static readonly PREFIX_BYTES = 4;

  private server: net.Server = {} as net.Server;
  private connections: [Address, net.Socket][] = [];
  private receivedMessages$: Subject<Buffer>;

  constructor(private port: number) {
    this.receivedMessages$ = new Subject();
    this.handleReceivedMessages();
  }

  close(): void {
    logger.info('Socket layer: closing.');
    this.server.close();
    this.receivedMessages$.complete();
    this.connections.forEach(([address, socket]) => socket.destroy());
  }

  getReceivedMessagesStream(): Observable<Buffer> {
    return this.receivedMessages$.asObservable();
  }

  send(config: Communication<Buffer>): Promise<void> {
    const { data, address } = config;
    const { host, port } = address;

    const prefixedData = prefixData(data);

    let client: net.Socket;

    const connected = find(
      compose(
        equals(address),
        nth(1)
      )
    )(this.connections);

    return new Promise((resolve, reject) => {
      if (connected) {
        client = (connected as any)[1];
        client.write(prefixedData, () => {
          logger.info('Socket layer: message sent!');
          resolve();
        });
      } else {
        client = new net.Socket();
        client.connect(
          port,
          host
        );

        client.on('connect', () => {
          this.connections = [...this.connections, [address, client]];
          client.write(prefixedData, () => {
            logger.info('Socket layer: message sent!');
            resolve();
          });
        });

        client.on('close', () => {
          this.connections = ramdaReject(
            compose(
              equals(address),
              nth(1)
            ),
            this.connections
          );
        });

        client.on('error', () => {
          logger.error('Socket layer: message not sent!');
          reject();
        });
      }
    });
  }

  private handleReceivedMessages(): void {
    logger.info(`Socket layer: Listening on port:${this.port}`);
    this.server = net.createServer((socket) => {
      let messageLength = 0;
      let message = Buffer.alloc(0);

      socket.on('data', (data: Buffer) => {
        message = Buffer.concat([message, data]);
        if (!messageLength && message.byteLength >= SocketLayer.PREFIX_BYTES) {
          messageLength = message.readUInt32BE(0);
        }
        if (message.byteLength >= messageLength + SocketLayer.PREFIX_BYTES) {
          logger.info('Socket layer: received new message!');
          this.receivedMessages$.next(
            message.slice(
              SocketLayer.PREFIX_BYTES,
              SocketLayer.PREFIX_BYTES + messageLength
            )
          );
          messageLength = 0;
          message = message.slice(SocketLayer.PREFIX_BYTES + messageLength);
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
