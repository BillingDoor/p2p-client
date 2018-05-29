// import { MessageParser, encodeMessage } from './message-parser';
import { Subject } from 'rxjs';
// import { prepareFindNodeMessage } from './protobuf-utils';
// import { Message } from '../protobuf/Message_pb';

import { StringDecoder } from 'string_decoder';
import { SocketLayer } from './socket-layer/socket-layer';

const receivedMessages = new Subject<Buffer>();

const peer = new SocketLayer(1337, receivedMessages);

peer.listen();

const node = new SocketLayer(2668, receivedMessages);
node.connectTo({ host: '127.0.0.1', port: 1337 });

// client.write('xD')
const decoder = new StringDecoder('utf8');

process.stdin.on('data', function(input: Buffer) {
  const text = decoder.write(input).trim();

  if (text == 'close') {
    peer.stopListening();
  }
});

// const dane = new Subject<Buffer>();

// const parser = new MessageParser(dane);

// parser.on(Message.MessageType.FIND_NODE, () => console.log('It works!'));
// parser.on(Message.MessageType.FIND_NODE, () => console.log('Hejka :D'));
// parser.on(Message.MessageType.FOUND_NODES, () => console.log('Nie powinno wyswietlic'));

// dane.next(
//   encodeMessage(
//     prepareFindNodeMessage({
//       sender: 1,
//       target: 2,
//       host: '123',
//       port: 23
//     })
//   )
// );
