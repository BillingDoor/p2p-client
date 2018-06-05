import { reject, equals, contains } from 'ramda';
import { first, tap, filter, delay } from 'rxjs/operators';
import { exec } from 'child_process';

import { Address, Contact } from '@models';
import { P2PLayer } from '@layers/p2p-layer/p2p-layer';
import { Message } from '@protobuf/Message_pb';
import logger from '@utils/logging';

export class BusinessLayer {
  private pingedNodes: Contact[] = [];

  constructor(private worker: P2PLayer) {
    this.handleMessages();
  }

  async joinNetwork(bootstrapNode: Address) {
    await this.worker.findNode({
      to: bootstrapNode
    });
  }

  close() {
    logger.info('Business layer: closing.');
    this.worker.leave();
    this.worker.close();
  }

  private handleMessages() {
    this.handleCommandMessage();
    this.handleCommandResponseMessage();
    this.handleFindNodeMessage();
    this.handleFoundNodesMessage();
    this.handleLeaveMessage();
    this.handlePingMessage();
    this.handlePingResponseMessage();
  }

  private handleCommandMessage() {
    this.worker
      .on(Message.MessageType.COMMAND)
      .pipe(
        tap(() => logger.info('Business layer: got command message')),
        tap(async (msg) => {
          const sender = checkSender(msg);
          const commandMsg = msg.getCommand();
          if (commandMsg) {
            if (commandMsg.getShouldrespond()) {
              exec(
                commandMsg.getCommand(),
                (error: Error | null, stdout: string, stderr: string) => {
                  let value: string;
                  let status: Message.Status;

                  if (error) {
                    value = stderr;
                    status = Message.Status.FAIL;
                  } else {
                    value = stdout;
                    status = Message.Status.OK;
                  }

                  logger.info(
                    `Business layer: Executed command '${commandMsg.getCommand()}'`
                  );
                  logger.info(
                    `Business layer: Command output:\n${
                      error ? stderr : stdout
                    }`
                  );

                  this.worker.commandResponse({
                    to: Contact.from(sender),
                    value,
                    status,
                    command: commandMsg.getCommand()
                  });
                }
              );
            } else {
              exec(
                commandMsg.getCommand(),
                (error: Error | null, stdout: string, stderr: string) => {
                  logger.info(
                    `Business layer: Executed command '${commandMsg.getCommand()}'`
                  );
                  logger.info(
                    `Business layer: Command output:\n${
                      error ? stderr : stdout
                    }`
                  );
                }
              );
            }
          } else {
            logger.error('Business layer: Command message not set.');
          }
        })
      )
      .subscribe();
  }

  private handleCommandResponseMessage() {
    this.worker
      .on(Message.MessageType.COMMAND_RESPONSE)
      .pipe(
        tap(() => logger.info('Business layer: got command response message'))
      )
      .subscribe();
  }

  private handleFindNodeMessage() {
    this.worker
      .on(Message.MessageType.FIND_NODE)
      .pipe(
        tap(() => logger.info('Business layer: got find node message')),
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

  private handleFoundNodesMessage() {
    this.worker
      .on(Message.MessageType.FOUND_NODES)
      .pipe(
        first(),
        tap(() => logger.info('Business layer: got found nodes message')),
        this.addNodeToRoutingTable(),
        this.pingNodes(),
        delay(5000),
        this.removeNotRespondingForPing()
      )
      .subscribe();
  }

  private handleLeaveMessage() {
    this.worker
      .on(Message.MessageType.LEAVE)
      .pipe(
        tap(() => logger.info('Business layer: got leave message')),
        tap((msg) => {
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
        tap(() => logger.info('Business layer: got ping message')),
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
        tap(() => logger.info('Business layer: got ping response message')),
        filter((msg) => {
          const sender = checkSender(msg);
          return contains(Contact.from(sender), this.pingedNodes);
        }),
        this.addNodeToRoutingTable(),
        tap((msg) => {
          const sender = checkSender(msg);
          this.pingedNodes = reject(
            equals(Contact.from(sender)),
            this.pingedNodes
          );
        })
      )
      .subscribe();
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

  private removeNotRespondingForPing() {
    return tap(() => {
      this.pingedNodes.forEach((node) =>
        this.worker.routingTable.removeNode(node)
      );
      this.pingedNodes = [];
    });
  }

  private addNodeToRoutingTable(node?: Address) {
    return tap((msg: Message) => {
      const sender = checkSender(msg);
      this.worker.routingTable.addNode(Contact.from(sender));
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
