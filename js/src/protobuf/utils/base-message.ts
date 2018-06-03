import { Contact } from '@models';

import { Message } from '../Message_pb';

export function prepareBaseMessage(config: {
  type: Message.MessageType;
  sender: Contact;
  receiver: Contact;
}): Message {
  const { sender, receiver, type } = config;
  const msg = new Message();

  msg.setUuid(Contact.generateGUID());
  msg.setSender(Contact.toMessageContact(sender));
  msg.setReceiver(Contact.toMessageContact(receiver));
  msg.setType(type);
  return msg;
}
