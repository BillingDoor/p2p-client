import { Observable, Subject } from 'rxjs';
import { map, filter } from 'rxjs/operators';

import { Communication } from '@models';
import { Message } from '../../protobuf/Message_pb';

export class MessageLayer {
  private messages$: Observable<Communication<Message>>;

  constructor(
    private inputMessages$: Observable<Communication<Buffer>>,
    private outputMessages$: Subject<Communication<Buffer>>
  ) {
    this.messages$ = this.inputMessages$.pipe(
      map(({ data, address }) => ({ data: decodeMessage(data), address }))
    );
  }

  on(type: Message.MessageType): Observable<Communication<Message>> {
    return this.messages$.pipe(filter((msg) => msg.data.getType() === type));
  }

  send(msg: Communication<Message>) {
    const { data, address } = msg;
    this.outputMessages$.next({ data: encodeMessage(data), address });
  }
}

export function decodeMessage(buffer: Buffer): Message {
  return Message.deserializeBinary(Uint8Array.from(buffer));
}

export function encodeMessage(msg: Message): Buffer {
  return Buffer.from(msg.serializeBinary());
}
