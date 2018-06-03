import asyncio
import logging.handlers
from python.StatusMessage import StatusMessage

logging.basicConfig(
    level=logging.DEBUG,
    format='%(name)s: %(message)s',
)
handler = logging.handlers.RotatingFileHandler(
    "client_log.txt",
    maxBytes=65536,
    backupCount=10,
)
formatter = logging.Formatter('%(name)s: %(message)s',)
handler.formatter = formatter
log = logging.getLogger(__name__)
log.addHandler(handler)

async def client(address, message):
    log.debug('connecting to {} port {}'.format(*address))
    try:
        reader, writer = await asyncio.open_connection(*address)
        writer.write(message)
        log.debug('sending {!r}'.format(message))
        if writer.can_write_eof():
            writer.write_eof()
        await writer.drain()
        log.debug('closing')
        writer.close()
        return StatusMessage.SUCCESS
    except Exception as error:
        log.warning("Client could not open connection: {}".format(error))
        return StatusMessage.FAILURE
