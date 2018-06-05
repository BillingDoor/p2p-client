import asyncio
import python.Protobuf.protobuf_utils as putils
import logging.handlers
import os

logging.basicConfig(
    level=logging.DEBUG,
    format='%(name)s: %(message)s',
)
handler = logging.handlers.RotatingFileHandler(
    os.path.abspath("./logs/log.txt"),
)
formatter = logging.Formatter('%(name)s: %(message)s',)
handler.formatter = formatter
log = logging.getLogger(__name__)
log.addHandler(handler)
log.propagate = False

class MessageLayer:
    def __init__(self, lower_layer):
        self.lower_layer = lower_layer

    async def add_layer_communication(self, higher, lower):
        """
        Adds means of communicating with lower and/or lower layer. Higher and lower should be a tuple of two objects
        that support asynchronous communication using get() and put() method to pass along data.
        Then starts listening on them for data.
        :param higher: Tuple of two objects for communication with higher layer
        :param lower: Tuple of two objects for communication with lower layer
        """
        self._higher = higher
        self._lower = lower
        asyncio.ensure_future(self._handle_lower_input())
        asyncio.ensure_future(self._handle_higher_input())

    async def _handle_lower_input(self):
        try:
            while True:
                log.debug("Waiting for message from lower layer")

                message = await self._lower[0].get()
                log.debug("Got message {!r}, handling it and sending it to higher layer".format(message))
                message = self._deserialize_message(message)

                await self._higher[1].put(message)
                log.debug("Message {!r} sent to the higher layer".format(message))

        except asyncio.CancelledError:
            log.debug("Caught CancelledError: Stop handling input from lower layer")
            return

    async def _handle_higher_input(self):
        try:
            while True:
                log.debug("Waiting for message from higher layer")

                message = await self._higher[0].get()
                log.debug("Got message {!r}; handling it and sending it to lower layer".format(message))
                message = self.handle_message_from_higher_layer(message)

                await self._lower[1].put(message)
                log.debug("Message {!r} sent to the lower layer".format(message))

        except asyncio.CancelledError:
            log.debug("Caught CancelledError: Stop handling input from higher layer")
            return

    def handle_message_from_lower_layer(self, message):
        """
        Prepare given message to pass it to higher layer
        :param message: message to prepare
        :return: Message to pass on to higher layer
        """
        return self._deserialize_message(message)

    async def start_server(self, ip, port):
        return await self.lower_layer.start_server(ip, port)

    async def stop_server(self):
        await self.lower_layer.stop_server()

    def handle_message_from_higher_layer(self, message):
        """
        Prepare given message to pass it to lower layer
        :param message: message to prepare
        :return: Message to pass on to lower layer
        """
        receiver_address = putils.get_receiver_address(message)
        return (self._serialize_message(message), receiver_address)

    def _deserialize_message(self, message):
        """
        Decode given message from string of bytes
        :param message: Message to decode
        :return: Decoded message
        """
        return putils.deserialize_message(message)

    def _serialize_message(self, message):
        """
        Encode given message to string of bytes
        :param message: Message to encode
        :return: Encoded message
        """
        return putils.serialize_message(message)