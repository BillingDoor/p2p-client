import { Contact } from '@models';

import { Message } from '../Message_pb';
import { prepareBaseMessage } from './base-message';

export function prepareFoundNodesMessage(config: {
  nodes: Message.Contact[];
  sender: Contact;
  receiver: Contact;
}) {
  const { nodes, sender, receiver } = config;

  const foundNodesMsg = new Message.FoundNodesMsg();
  foundNodesMsg.setNodesList(nodes);

  const msg = prepareBaseMessage({
    type: Message.MessageType.FOUND_NODES,
    sender,
    receiver
  });
  msg.setFoundnodes(foundNodesMsg);

  return msg;
}
