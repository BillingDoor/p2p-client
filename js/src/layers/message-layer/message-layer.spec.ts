import { Subject } from 'rxjs';

import { Communication } from '@models';
import { Message } from '../../protobuf/Message_pb';

import { MessageLayer } from './message-layer';

describe('Layer: MessageLayer', function() {
  let inputMessages: Subject<Communication<Buffer>>;
  let outputMessages: Subject<Communication<Buffer>>;
  let layer: MessageLayer;

  beforeEach(function() {
    inputMessages = new Subject();
    outputMessages = new Subject();
    layer = new MessageLayer(inputMessages.asObservable(), outputMessages);
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
        layer.on(Message.MessageType.FIND_NODE).subscribe(firstCallback);
        layer.on(Message.MessageType.FIND_NODE).subscribe(secondCallback);

        sendMessage();

        expect(firstCallback).toHaveBeenCalled();
        expect(secondCallback).toHaveBeenCalled();
      });

      it('should not invoke callback listening on other type', function() {
        layer.on(Message.MessageType.PING).subscribe(otherTypeCallback);

        sendMessage();

        expect(otherTypeCallback).not.toHaveBeenCalled();
      });
    });
  });

  function sendMessage() {
    inputMessages.next({
      data: new Buffer('foo'),
      address: {
        host: 'foo',
        port: 123
      }
    });
  }
});
