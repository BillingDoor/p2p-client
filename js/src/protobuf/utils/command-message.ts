import { Contact } from '@models';

import { Message } from '../Message_pb';
import { prepareBaseMessage } from './base-message';

export function prepareCommandMessage(config: {
  command: string;
  shouldRespond: boolean;
  sender: Contact;
  receiver: Contact;
}) {
  const { command, shouldRespond, sender, receiver } = config;

  const commandMsg = new Message.CommandMsg();
  commandMsg.setCommandstring(command);
  commandMsg.setSendresponse(shouldRespond);

  const msg = prepareBaseMessage({
    type: Message.MessageType.FIND_NODE,
    sender,
    receiver
  });
  msg.setFindnode(commandMsg);

  return msg;
}
