import { Message } from '../../protobuf/Message_pb';
import { Observable, Subject } from 'rxjs';
import { map, tap } from 'rxjs/operators';

export class MessageParser {
  private messages$: Observable<Message>;
  private subscribers: Function[][];

  private static types = Object.keys(Message.MessageType).length;

  constructor(
    private messageData$: Observable<Buffer>,
    private messagesToSend$: Subject<Buffer>
  ) {
    this.subscribers = Array.from({ length: MessageParser.types }, () => []);
    this.messages$ = this.messageData$.pipe(
      map(decodeMessage),
      tap(this.triggerHandlers())
    );
    this.messages$.subscribe();
  }

  on(type: Message.MessageType, cb: Function): void {
    this.subscribers[type] = [...this.subscribers[type], cb];
  }

  send(msg: Message, node: string) {
    this.messagesToSend$.next(encodeMessage(msg));
  }

  private triggerHandlers() {
    return (msg: Message) => {
      const handlers = this.subscribers[msg.getType()];
      handlers.forEach((fn) => fn(msg));
    };
  }
}

export function decodeMessage(buffer: Buffer): Message {
  return Message.deserializeBinary(Uint8Array.from(buffer));
}

export function encodeMessage(msg: Message): Buffer {
  return Buffer.from(msg.serializeBinary());
}
