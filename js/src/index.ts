import { StringDecoder } from 'string_decoder';

import { Contact } from '@models';
import { SocketLayer } from '@layers/socket-layer/socket-layer';
import { MessageLayer } from '@layers/message-layer/message-layer';
import { P2PLayer } from '@layers/p2p-layer/p2p-layer';
import { BusinessLayer } from '@layers/business-layer/business-layer';
import { ApplicationLayer } from '@layers/application-layer/application-layer';

const bootstrapNode = spawnNode(1337);
const nodes = [spawnNode(1338), spawnNode(1339), spawnNode(1340)];

nodes.forEach((node) =>
  node.launch({
    host: 'localhost',
    port: 1337
  })
);

const decoder = new StringDecoder('utf8');

process.stdout.write('> ');

process.stdin.on('data', function(input: Buffer) {
  const text = decoder.write(input).trim();

  if (text == 'close') {
    process.stdout.write('Closing application...\n');
    bootstrapNode.close();
    // nodes.forEach((node) => node.close());
    // process.exit();
  } else {
    process.stdout.write('> ');
  }
});

function spawnNode(port: number) {
  const me = new Contact({
    address: {
      host: 'localhost',
      port
    }
  });

  const socketLayer = new SocketLayer(me.address.port);
  const messageLayer = new MessageLayer(socketLayer);
  const p2pLayer = new P2PLayer(messageLayer, me);
  const businessLayer = new BusinessLayer(p2pLayer, me);
  return new ApplicationLayer(businessLayer);
}

// TODO: pretty debug logs
// TODO: divide protobuf/utils into separate files
// TODO: handle errors
// * bootstrapNode not listening / not available
// * wait on closing until all messages are sent
