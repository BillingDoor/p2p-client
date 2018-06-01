import { equals } from 'ramda';
import { Observable, Subject } from 'rxjs';
import { map, filter } from 'rxjs/operators';

import { Communication, Contact } from '@models';
import { Message } from '@protobuf/Message_pb';

export class MessageLayer {
  private messages$: Observable<Message>;

  constructor(
    private receivedMessages$: Observable<Buffer>,
    private messagesToSend$: Subject<Communication<Buffer>>
  ) {
    this.messages$ = this.receivedMessages$.pipe(map(decodeMessage));
  }

  on(type: Message.MessageType): Observable<Message> {
    return this.messages$.pipe(filter((msg) => msg.getType() === type));
  }

  send(msg: Message) {
    const sender = msg.getSender();
    const receiver = msg.getReceiver();

    if (!sender || !receiver) {
      throw new Error('Message layer: Invalid message sender/receiver.');
    }

    const { address: senderAddress } = Contact.from(sender);
    const { address } = Contact.from(receiver);

    const msgToSelf = equals(senderAddress, address);
    if (msgToSelf) {
      throw new Error('Message layer: Cannot send message to self.');
    }

    this.messagesToSend$.next({ data: encodeMessage(msg), address });
  }

  close() {
    this.messagesToSend$.complete();
    this.worker.close();
  }
}

export function decodeMessage(buffer: Buffer): Message {
  return Message.deserializeBinary(Uint8Array.from(buffer));
}

export function encodeMessage(msg: Message): Buffer {
  return Buffer.from(msg.serializeBinary());
}
