import { Contact } from '../contact/contact';

import { Message } from '../../protobuf/Message_pb';

export function prepareBaseMessage(contact: Contact): Message {
  const msg = new Message();
  msg.setSender(`${contact.host}:${contact.port}`);
  msg.setUuid(contact.guid);
  return msg;
}

export function prepareFindNodeMessage(config: {
  sender: number;
  target: number;
  host: string;
  port: number;
}) {
  const { sender, target, host, port } = config;

  const targetGUID = new Message.FindNode();
  targetGUID.setGuid(target);

  const msg = prepareBaseMessage(new Contact({ guid: sender, host, port }));
  msg.setType(Message.MessageType.FIND_NODE);
  msg.setPfindnode(targetGUID);

  return msg;
}


