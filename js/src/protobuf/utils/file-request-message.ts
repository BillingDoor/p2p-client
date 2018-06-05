import { Contact } from '@models';

import { Message } from '../Message_pb';
import { prepareBaseMessage } from './base-message';

export function prepareFileRequestMessage(config: {
  path: string;
  sender: Contact;
  receiver: Contact;
}) {
  const { path, sender, receiver } = config;

  const fileRequestMsg = new Message.FileRequestMsg();
  fileRequestMsg.setPath(path);

  const msg = prepareBaseMessage({
    type: Message.MessageType.COMMAND,
    sender,
    receiver
  });
  msg.setFilerequest(fileRequestMsg);

  return msg;
}
