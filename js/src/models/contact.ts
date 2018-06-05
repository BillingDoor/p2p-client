import { Message } from '@protobuf/Message_pb';
import { generateID } from '@utils/random-id';

import { Address } from './address';

export class Contact {
  address: Address;
  guid: string;
  isNAT: boolean;

  constructor(config: { address: Address; guid?: string; isNAT?: boolean }) {
    const { address, guid, isNAT } = config;

    this.address = address;
    this.guid = guid || generateID();
    this.isNAT = isNAT || false;
  }

  static toMessageContact(contact: Contact): Message.Contact {
    const messageContact = new Message.Contact();
    messageContact.setIp(contact.address.host);
    messageContact.setPort(contact.address.port);
    messageContact.setGuid(contact.guid);
    messageContact.setIsnat(contact.isNAT);
    return messageContact;
  }

  static from(contact: Message.Contact): Contact {
    const host = contact.getIp();
    const port = contact.getPort();
    const guid = contact.getGuid();
    const isNAT = contact.getIsnat();

    return new Contact({
      address: {
        host,
        port
      },
      guid,
      isNAT
    });
  }
}
