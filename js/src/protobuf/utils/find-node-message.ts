import { Contact } from '@models';

import { Message } from '../Message_pb';
import { prepareBaseMessage } from './base-message';

export function prepareFindNodeMessage(config: {
  node: string;
  sender: Contact;
  receiver: Contact;
}) {
  const { node, sender, receiver } = config;

  const findNodeMsg = new Message.FindNodeMsg();
  findNodeMsg.setGuid(node);

  const msg = prepareBaseMessage({
    type: Message.MessageType.FIND_NODE,
    sender,
    receiver
  });
  msg.setFindnode(findNodeMsg);

  return msg;
}
