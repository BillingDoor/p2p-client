export class Contact {
  guid: number;
  host: string;
  port: number;

  constructor(config: { host: string; port: number }) {
    const { host, port } = config;

    this.guid = Contact.generateGUID();
    this.host = host;
    this.port = port;
  }

  static generateGUID() {
    return Math.floor(Math.random() * 2 ** 32);
  }
}
