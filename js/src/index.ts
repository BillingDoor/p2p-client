import { Peer } from './peer';
import { StringDecoder } from 'string_decoder';

// import { Message } from '../protobuf/Message_pb';

const peer = new Peer({ host: '127.0.0.1', port: 1337 });

peer.runServer();

const node = new Peer({ host: '127.0.0.1', port: 2668 });
node.connectTo({ host: '127.0.0.1', port: 1337 });

const decoder = new StringDecoder('utf8');

process.stdin.on('data', function(input: Buffer) {
  const text = decoder.write(input).trim();

  if (text == 'close') {
    peer.stopServer();
  }
});
