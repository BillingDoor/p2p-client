import { Contact } from '@models';

import { Message } from '../Message_pb';
import { prepareBaseMessage } from './base-message';
import { generateID } from '@utils/random-id';

export function prepareFileChunkMessage(config: {
  fileName: string;
  fileSize: number;
  ordinal: number;
  data: Buffer;
  sender: Contact;
  receiver: Contact;
}) {
  const { fileName, fileSize, ordinal, data, sender, receiver } = config;
  const uuid = generateID();

  const fileChunkMsg = new Message.FileChunkMsg();
  fileChunkMsg.setUuid(uuid);
  fileChunkMsg.setFilename(fileName);
  fileChunkMsg.setFilesize(fileSize);
  fileChunkMsg.setOrdinal(ordinal);
  fileChunkMsg.setData(Uint8Array.from(data));

  const msg = prepareBaseMessage({
    type: Message.MessageType.COMMAND,
    sender,
    receiver
  });
  msg.setFilechunk(fileChunkMsg);

  return msg;
}
