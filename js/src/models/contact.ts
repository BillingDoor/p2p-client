import * as bigInt from 'big-integer';

import { Address } from '@models';
import { Message } from '@protobuf/Message_pb';

export class Contact {
  address: Address;
  guid: string;
  isNAT: boolean;

  constructor(config: { address: Address; guid?: string; isNAT?: boolean }) {
    const { address, guid, isNAT } = config;

    this.address = address;
    this.guid = guid || Contact.generateGUID();
    this.isNAT = isNAT || false;
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

  static generateGUID() {
    return bigInt.randBetween(0, 2 ** 64).toString();
  }
}
