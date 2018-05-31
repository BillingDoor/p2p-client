import { Contact, Address } from '@models';
import { Message } from '../protobuf/Message_pb';

export function prepareBaseMessage(contact: Contact): Message {
  const msg = new Message();
  msg.setSender(`${contact.host}:${contact.port}`);
  msg.setUuid(Number(contact.guid));
  return msg;
}

export function prepareFindNodeMessage(config: {
  sender: string;
  target: string;
  address: Address;
}) {
  const { sender, target, address: {host, port} } = config;

  const targetGUID = new Message.FindNode();
  targetGUID.setGuid(Number(target));

  const msg = prepareBaseMessage(new Contact({ guid: sender, host, port }));
  msg.setType(Message.MessageType.FIND_NODE);
  msg.setPfindnode(targetGUID);

  return msg;
}

export function preparePingMessage(config: {
  sender: string;
  target: string;
  host: string;
  port: number;
}) {
  const { sender, target, host, port } = config;

  const targetGUID = new Message.FindNode();
  targetGUID.setGuid(Number(target));

  const msg = prepareBaseMessage(new Contact({ guid: sender, host, port }));
  msg.setType(Message.MessageType.FIND_NODE);
  msg.setPfindnode(targetGUID);

  return msg;
}
