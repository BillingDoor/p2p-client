import { Contact } from '@models';
import { Message } from '../Message_pb';

export function prepareBaseMessage(config: {
  type: Message.MessageType;
  sender: Contact;
  receiver: Contact;
}): Message {
  const { sender, receiver, type } = config;
  const msg = new Message();

  msg.setSender(sender.toMessageContact());
  msg.setReceiver(sender.toMessageContact());
  msg.setType(type);
  return msg;
}

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
