import { Contact } from './contact/contact';

export class P2P {
  executeCommandOn(node: Contact) {}
  listPeers() {}
  listFileFrom(node: Contact) {}
  sendFileTo(file: string, node: Contact) {}
  sendFileToAll(file: string) {}
  on(type: string, cb: Function): void {
    switch (type) {
      case 'file':
        this.onFile(cb);
        break;
      case 'command':
        this.onCommand(cb);
        break;
    }
  }
  private onFile(cb: Function): void {}
  private onCommand(cb: Function): void {}
}
