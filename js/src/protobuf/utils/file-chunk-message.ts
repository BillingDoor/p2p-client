import { Contact } from '@models';

import { Message } from '../Message_pb';
import { prepareBaseMessage } from './base-message';

export function prepareFileChunkMessage(config: {
  uuid: string;
  fileName: string;
  fileSize: number;
  ordinal: number;
  data: Buffer;
  sender: Contact;
  receiver: Contact;
}) {
  const { uuid, fileName, fileSize, ordinal, data, sender, receiver } = config;

  const fileChunkMsg = new Message.FileChunkMsg();
  fileChunkMsg.setUuid(uuid);
  fileChunkMsg.setFilename(fileName);
  fileChunkMsg.setFilesize(fileSize);
  fileChunkMsg.setOrdinal(ordinal);
  fileChunkMsg.setData(Uint8Array.from(data));

  const msg = prepareBaseMessage({
    type: Message.MessageType.FILE_CHUNK,
    sender,
    receiver
  });
  msg.setFilechunk(fileChunkMsg);

  return msg;
}
