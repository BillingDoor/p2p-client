import { Contact } from '@models';
import { Message } from '../Message_pb';

export function prepareBaseMessage(config: {
  sender: Contact;
  type: Message.MessageType;
}): Message {
  const { sender, type } = config;
  const msg = new Message();
  const cnt = new Message.Contact();
  cnt.setGuid(sender.guid);
  cnt.setIsnat(sender.isNAT);
  cnt.setIp(sender.address.host);
  cnt.setPort(sender.address.port);
  msg.setSender(cnt);
  msg.setType(type);
  return msg;
}

export function prepareFindNodeMessage(config: {
  node: string;
  sender: Contact;
}) {
  const { node, sender } = config;

  const findNodeMsg = new Message.FindNodeMsg();
  findNodeMsg.setGuid(node);

  const msg = prepareBaseMessage({
    sender,
    type: Message.MessageType.FIND_NODE
  });
  msg.setFindnode(findNodeMsg);

  return msg;
}

export function prepareFoundNodesMessage(config: {
  nodes: Message.Contact[];
  sender: Contact;
}) {
  const { nodes, sender } = config;

  const foundNodesMsg = new Message.FoundNodesMsg();
  foundNodesMsg.setNodesList(nodes);

  const msg = prepareBaseMessage({
    sender,
    type: Message.MessageType.FOUND_NODES
  });
  msg.setFoundnodes(foundNodesMsg);

  return msg;
}
