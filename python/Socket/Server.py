import asyncio
import socket
import logging.handlers

logging.basicConfig(
    level=logging.DEBUG,
    format='%(name)s: %(message)s',
)
handler = logging.handlers.RotatingFileHandler(
    "server_log.txt",
    maxBytes=65536,
    backupCount=10,
)
formatter = logging.Formatter('%(name)s: %(message)s',)
handler.formatter = formatter
log = logging.getLogger(__name__)
log.addHandler(handler)

QUEUE = None
MAIN_LOOP = None

async def receive_data(reader, writer):
    try:
        address = writer.get_extra_info('peername')
        log.debug("connection with {} accepted".format(address))
        while True:
            data = await reader.read()
            if data:
                log.debug("received {!r}".format(data))
                # create coroutine to put data into the queue
                put_cor = QUEUE.put(data)
                # send that coroutine to the main loop
                asyncio.run_coroutine_threadsafe(put_cor, MAIN_LOOP)
            else:
                log.debug('closing connection with {}'.format(address))
                writer.close()
                return
    except asyncio.CancelledError:
        log.warning("Server was stopped while reading data.")
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
        log.warning("Could not start the server: {}".format(error))

    log.debug("Starting up server on {}:{}".format(ip, port))
    loop.run_until_complete(_monitor(stop_server_event, server))
    log.debug("Server event loop stopped running")
    loop.close()

