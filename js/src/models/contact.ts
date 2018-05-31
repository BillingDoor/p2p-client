import * as bigInt from 'big-integer';

export class Contact {
  host: string;
  port: number;
  guid: string;
  isNAT: boolean;

  constructor(config: {
    host: string;
    port: number;
    guid?: string;
    isNAT?: boolean;
  }) {
    const { host, port, guid, isNAT } = config;

    this.host = host;
    this.port = port;
    this.guid = guid || Contact.generateGUID();
    this.isNAT = isNAT || false;
  }

  static generateGUID() {
    return bigInt.randBetween(0, 2 ** 64).toString();
  }
}
