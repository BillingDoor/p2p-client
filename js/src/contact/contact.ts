import * as bigInt from 'big-integer';

export class Contact {
  host: string;
  port: number;
  guid: number;
  isNAT: boolean;

  constructor(config: {
    host: string;
    port: number;
    guid?: number;
    isNAT?: boolean;
  }) {
    const { host, port, guid, isNAT } = config;

    this.guid = guid || Contact.generateGUID();
    this.host = host;
    this.port = port;
    this.isNAT = isNAT || false;
  }

  static generateGUID() {
    return bigInt.randBetween(0, 2 ** 63).toJSNumber();
  }
}
