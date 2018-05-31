import { Subject } from 'rxjs';

import { StringDecoder } from 'string_decoder';
import { SocketLayer } from './layers/socket-layer/socket-layer';
import { Communication } from './models';
import { MessageLayer } from './layers/message-layer/message-layer';
import { prepareFindNodeMessage } from './protobuf-utils';
import { Message } from './protobuf/Message_pb';

const receivedMessages$ = new Subject<Communication<Buffer>>();
const messagesToSend$ = new Subject<Communication<Buffer>>();

const socketLayer = new SocketLayer(
  1337,
  receivedMessages$,
  messagesToSend$.asObservable()
);

const messageLayer = new MessageLayer(
  receivedMessages$.asObservable(),
  messagesToSend$
);

const receivedMessages2$ = new Subject<Communication<Buffer>>();
const messagesToSend2$ = new Subject<Communication<Buffer>>();

const socketLayer2 = new SocketLayer(
  3424,
  receivedMessages2$,
  messagesToSend2$.asObservable()
);
void socketLayer2;

const messageLayer2 = new MessageLayer(
  receivedMessages2$.asObservable(),
  messagesToSend2$
);

messageLayer2
  .on(Message.MessageType.FIND_NODE)
  .subscribe(() => console.log('It works!'));

messageLayer.send({
  data: prepareFindNodeMessage({
    sender: '1',
    target: '2',
    host: '123',
    port: 23
  }),
  address: {
    host: 'localhost',
    port: 3424
  }
});

const decoder = new StringDecoder('utf8');

process.stdin.on('data', function(input: Buffer) {
  const text = decoder.write(input).trim();

  if (text == 'close') {
    socketLayer.close();
  }
});
