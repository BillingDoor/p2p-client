import { Observable, Subject } from 'rxjs';
import { map, filter } from 'rxjs/operators';

import { Communication, Contact } from '@models';
import { Message } from '../../protobuf/Message_pb';

export class MessageLayer {
  private messages$: Observable<Message>;

  constructor(
    private inputMessages$: Observable<Buffer>,
    private outputMessages$: Subject<Communication<Buffer>>
  ) {
    this.messages$ = this.inputMessages$.pipe(map(decodeMessage));
  }

  on(type: Message.MessageType): Observable<Message> {
    return this.messages$.pipe(filter((msg) => msg.getType() === type));
  }

  send(msg: Message) {
    const receiver = msg.getReceiver();
    if (receiver) {
      const { address } = Contact.from(receiver);
      this.outputMessages$.next({ data: encodeMessage(msg), address });
    } else {
      throw new Error('Message layer: Message receiver not set.');
    }
  }
}

export function decodeMessage(buffer: Buffer): Message {
  return Message.deserializeBinary(Uint8Array.from(buffer));
}

export function encodeMessage(msg: Message): Buffer {
  return Buffer.from(msg.serializeBinary());
}
