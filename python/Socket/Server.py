import asyncio
import socket
import logging.handlers
import struct

logging.basicConfig(
    level=logging.DEBUG,
    format='%(name)s: %(message)s',
)
handler = logging.handlers.RotatingFileHandler(
    "./logs/server_log.txt",
    maxBytes=65536,
    backupCount=10,
)
formatter = logging.Formatter('%(name)s: %(message)s',)
handler.formatter = formatter
log = logging.getLogger(__name__)
log.addHandler(handler)
log.propagate = False

QUEUE = None
MAIN_LOOP = None

async def receive_data(reader, writer):
    try:
        address = writer.get_extra_info('peername')
        log.debug("connection with {} accepted".format(address))

        # First we read length
        message_length = 0
        try:
            data = await reader.readexactly(4)
            if data == '':
                log.warning("Connection with {} was unexpectedly closed".format(address))
                writer.close()
                return
            else:
                message_length = struct.unpack('>L', data)[0]

                # Read proper message
                data = await reader.readexactly(message_length)
                if data:
                    log.debug("received {!r}".format(data))
                    # create coroutine to put data into the queue
                    put_cor = QUEUE.put(data)
                    # send that coroutine to the main loop
                    asyncio.run_coroutine_threadsafe(put_cor, MAIN_LOOP)
                else:
                    log.warning("Connection with {} was unexpectedly closed".format(address))
                    writer.close()
                    return
        except asyncio.IncompleteReadError:
            log.warning("Connection with {} was closed before we got entire message".format(address))
            writer.close()
            return
    except socket.error as error:
        log.warning("Socket error: {}".format(error))
    except asyncio.CancelledError:
        log.warning("Server was stopped while reading data.")
    finally:
        writer.close()

async def _monitor(stop_server_event, server):
    log.debug("Start monitoring stop_server_event")
    await asyncio.get_event_loop().run_in_executor(None, func=stop_server_event.wait)
    log.debug("Stop_server_event was triggered. Proceed to close the server.")
    server.close()
    await server.wait_closed()
    log.debug("Server closed")


def run_server(ip, port, stop_server_event, queue, main_loop):
    global QUEUE
    QUEUE = queue
    global MAIN_LOOP
    MAIN_LOOP = main_loop
    asyncio.set_event_loop(asyncio.new_event_loop())

    loop = asyncio.get_event_loop()
    factory = asyncio.start_server(receive_data, ip, port)
    try:
        server = loop.run_until_complete(factory)
    except socket.error as error:
        return

    log.debug("Starting up server on {}:{}".format(ip, port))
    loop.run_until_complete(_monitor(stop_server_event, server))
    log.debug("Server event loop stopped running")
    loop.close()

