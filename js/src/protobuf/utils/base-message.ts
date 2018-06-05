import { Contact } from '@models';
import { generateID } from '@utils/random-id';

import { Message } from '../Message_pb';

export function prepareBaseMessage(config: {
  type: Message.MessageType;
  sender: Contact;
  receiver: Contact;
}): Message {
  const { sender, receiver, type } = config;
  const msg = new Message();

  msg.setUuid(generateID());
  msg.setSender(Contact.toMessageContact(sender));
  msg.setReceiver(Contact.toMessageContact(receiver));
  msg.setType(type);
  return msg;
}
