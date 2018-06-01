import * as net from 'net';
import { Subject, Observable } from 'rxjs';
import { Communication } from '@models';
import { takeUntil, tap } from 'rxjs/operators';

export class SocketLayer {
  private server: net.Server = {} as net.Server;
  private closed$ = new Subject<void>();

  constructor(
    private port: number,
    private messages$: Subject<Buffer>,
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
    console.log('Socket layer: closing connections.');
    this.server.close();
    this.closed$.next();
    this.closed$.complete();
    this.messages$.complete();
  }

  private handleMessagesToSend() {
    this.messagesToSend$
      .pipe(takeUntil(this.closed$), tap((msg) => this.send(msg)))
      .subscribe();
  }

  private listen(): void {
    console.log(`Socket layer: Listening on port:${this.port}`);
    this.server = net.createServer((socket) => {
      socket.on('data', (data: Buffer) => {
        console.log('Socket layer: new message!');
        this.messages$.next(data);
        socket.destroy();
      });
    });
    this.server.listen(this.port);
  }
}
