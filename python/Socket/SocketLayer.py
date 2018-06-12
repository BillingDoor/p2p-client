import asyncio
import threading
import logging.handlers
from python.utils.StatusMessage import StatusMessage
from python.Socket.Server import run_server
from python.Socket.Client import client
import os
import struct

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
log.propagate=False

class SocketLayer:
    def __init__(self):
        self.server_monitor = None

    async def add_layer_communication(self, higher):
        """
        Adds means of communicating with lower and/or lower layer. Higher and lower should be a tuple of two objects
        that support asynchronous communication using get() and put() method to pass along data.
        Then starts listening on them for data.
        :param higher: Tuple of two objects for communication with higher layer
        """
        self._higher = higher
        asyncio.ensure_future(self._handle_higher_input())

    async def _handle_higher_input(self):
        try:
            while True:
                log.debug("Waiting for message from higher layer")
                message = await self._higher[0].get()
                log.debug("Got message {!r}; handling it and sending to the target receiver".format(message))
                status = await self.handle_message_from_higher_layer(message)
                if status is StatusMessage.FAILURE:
                    log.warning("Message {!r} was not handled properly".format(message))

        except asyncio.CancelledError:
            log.debug("Caught CancelledError: Stop handling input from higher layer")
            return

    async def handle_message_from_higher_layer(self, message):
        """
        Handles message and sends it to the receiver
        :param message: message to send
        :return: SUCCESS or FAILURE
        """
        peer_address = message[1]
        serialized_message = message[0]
        framed_message = self._frame_message(serialized_message)
        status = await client(peer_address, framed_message)
        return status

    def _frame_message(self, message):
        """
        Frames message by adding the beginning length of it
        :param message: Message to frame
        :return: Framed message
        """
        packed_len = struct.pack('>L', len(message))
        return packed_len + message

    async def start_server(self, ip, port):
        self.stop_server_event = threading.Event()
        self.server_thread = threading.Thread(target=run_server,
                                              name="Server({}:{}) Thread".format(ip, port),
                                              args=(ip, port, self.stop_server_event,
                                                    self._higher[1], asyncio.get_event_loop())
                                              )
        self.server_thread.start()
        print("Wait for server to start")
        log.debug("Waiting 4 seconds to check if server started")
        await asyncio.sleep(4)

        if self.server_thread.is_alive():
            log.debug("Started server thread on {}:{}".format(ip, port))
            print("Started server thread on {}:{}".format(ip, port))
            self.server_monitor = asyncio.ensure_future(self._monitor_server_thread())
            return StatusMessage.SUCCESS
        else:
            log.warning("Could not start server on {}:{}".format(ip, port))
            return StatusMessage.FAILURE

    async def stop_server(self):
        if self.server_monitor:
            self.server_monitor.cancel()
            status = await self.server_monitor
            self.server_monitor = None
            return status
        else:
            log.warning("Cannot stop the server because it is not running")
            return StatusMessage.FAILURE

    async def _monitor_server_thread(self):
        """
        Wait for server thread to join, and in case of force closing make sure to stop the server
        """
        try:
            await asyncio.get_event_loop().run_in_executor(None, self.server_thread.join)
        except asyncio.CancelledError:
            # If we get cancelled error we
            log.debug("Got CancelledError. Set Event to stop the server.")
            self.stop_server_event.set()
            log.debug("Wait 4 seconds for server to join.")
            self.server_thread.join(timeout=4)
            if self.server_thread.is_alive():
                log.debug("Server did not stop correctly after setting up event. Attempt to kill it.")
                result = self._kill_server()
                if result is StatusMessage.FAILURE:
                    log.critical("Server did not stop properly and an attempt to kill it failed.")
                    return result
            log.debug("Server stopped and joined correctly")
            return StatusMessage.SUCCESS

