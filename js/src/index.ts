import { Subject } from 'rxjs';

// import { StringDecoder } from 'string_decoder';
import { SocketLayer } from './layers/socket-layer/socket-layer';
import { Communication, Contact } from './models';
import { MessageLayer } from './layers/message-layer/message-layer';
import { P2PLayer } from '@layers/p2p-layer/p2p-layer';
import { BusinessLayer } from '@layers/business-layer/business-layer';
import { ApplicationLayer } from '@layers/application-layer/application-layer';

const peer1 = spawnNode(1337);
const peer2 = spawnNode(2345);
void peer2;
peer1.launchClient({
  host: 'localhost',
  port: 2345
});

// const decoder = new StringDecoder('utf8');

// process.stdin.on('data', function(input: Buffer) {
//   const text = decoder.write(input).trim();

//   if (text == 'close') {
//     socketLayer.close();
//   }
// });

function spawnNode(port: number) {
  const receivedMessages$ = new Subject<Buffer>();
  const messagesToSend$ = new Subject<Communication<Buffer>>();

  const me = new Contact({
    address: {
      host: 'localhost',
      port
    }
  });

  const socketLayer = new SocketLayer(
    me.address.port,
    receivedMessages$,
    messagesToSend$.asObservable()
  );

  void socketLayer;

  const messageLayer = new MessageLayer(
    receivedMessages$.asObservable(),
    messagesToSend$
  );

  const p2pLayer = new P2PLayer(messageLayer, me);
  const businessLayer = new BusinessLayer(p2pLayer, me);
  return new ApplicationLayer(businessLayer);
}
