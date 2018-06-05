import { Contact } from '@models';

import { Message } from '../Message_pb';
import { prepareBaseMessage } from './base-message';

export function prepareCommandResponseMessage(config: {
  value: string;
  status: Message.Status;
  command: string;
  sender: Contact;
  receiver: Contact;
}) {
  const { value, status, command, sender, receiver } = config;

  const commandMsg = new Message.CommandResponseMsg();
  commandMsg.setValue(value);
  commandMsg.setStatus(status);
  commandMsg.setCommand(command);

  const msg = prepareBaseMessage({
    type: Message.MessageType.COMMAND_RESPONSE,
    sender,
    receiver
  });
  msg.setResponse(commandMsg);

  return msg;
}
