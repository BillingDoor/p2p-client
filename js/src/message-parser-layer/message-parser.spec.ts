import { Subject } from 'rxjs';

import { Message } from '../../protobuf/Message_pb';
import { prepareFindNodeMessage } from '../protobuf-utils';
import { MessageParser, encodeMessage } from './message-parser';

describe('Layer: MessageParser', function() {
  let inputMessages: Subject<Buffer>;
  let outputMessages: Subject<Buffer>;
  let layer: MessageParser;

  beforeEach(function() {
    inputMessages = new Subject();
    outputMessages = new Subject();
    layer = new MessageParser(inputMessages, outputMessages);
  });

  describe('Method: "on"', function() {
    it('should exist', function() {
      const result = typeof layer.on === 'function';
      expect(result).toBe(true);
    });

    describe('When: message of type "FIND_NODE" is in stream', function() {
      let firstCallback: jasmine.Spy;
      let secondCallback: jasmine.Spy;
      let otherTypeCallback: jasmine.Spy;

      beforeEach(function() {
        firstCallback = jasmine.createSpy('firstCallback');
        secondCallback = jasmine.createSpy('secondCallback');
        otherTypeCallback = jasmine.createSpy('otherTypeCallback');
      });

      it('should invoke callbacks listening on that type', function() {
        layer.on(Message.MessageType.FIND_NODE, firstCallback);
        layer.on(Message.MessageType.FIND_NODE, secondCallback);

        sendMessage();

        expect(firstCallback).toHaveBeenCalled();
        expect(secondCallback).toHaveBeenCalled();
      });

      it('should not invoke callback listening on other type', function() {
        layer.on(Message.MessageType.PING, otherTypeCallback);

        sendMessage();

        expect(otherTypeCallback).not.toHaveBeenCalled();
      });
    });
  });

  function sendMessage() {
    inputMessages.next(
      encodeMessage(
        prepareFindNodeMessage({
          sender: 1,
          target: 2,
          host: '123',
          port: 23
        })
      )
    );
  }
});
