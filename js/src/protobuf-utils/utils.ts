import { Contact, Address } from '@models';
import { Message } from '../protobuf/Message_pb';

export function prepareBaseMessage(contact: Contact): Message {
  const msg = new Message();
  msg.setSender(`${contact.address.host}:${contact.address.port}`);
  msg.setUuid(contact.guid);
  return msg;
}

export function prepareFindNodeMessage(config: {
  senderGUID: string;
  targetGUID: string;
  address: Address;
}) {
  const { senderGUID, targetGUID, address } = config;

  const findNodeMsg = new Message.FindNodeMsg();
  findNodeMsg.setGuid(targetGUID);

  const msg = prepareBaseMessage(new Contact({ address, guid: senderGUID }));
  msg.setType(Message.MessageType.FIND_NODE);
  msg.setFindnode(findNodeMsg);

  return msg;
}

export function prepareFoundNodesMessage(config: {
  senderGUID: string;
  targetGUID: string;
  address: Address;
}) {
  const { senderGUID, address } = config;

  const foundNodesMsg = new Message.FoundNodesMsg();
  foundNodesMsg.setNodesList([]);

  const msg = prepareBaseMessage(new Contact({ address, guid: senderGUID }));
  msg.setType(Message.MessageType.FOUND_NODES);
  msg.setFoundnodes(foundNodesMsg);

  return msg;
}

export function preparePingMessage(config: {
  senderGUID: string;
  targetGUID: string;
  address: Address;
}) {
  const { senderGUID, targetGUID, address } = config;

  const pingMsg = new Message.PingMsg();
  pingMsg.setGuid(targetGUID);

  const msg = prepareBaseMessage(new Contact({ address, guid: senderGUID }));
  msg.setType(Message.MessageType.PING);
  msg.setPing(pingMsg);

  return msg;
}
