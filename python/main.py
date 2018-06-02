from python.Socket.SocketLayer import SocketLayer
from python.P2P.P2PLayer import P2PLayer
from python.Message.MessageLayer import MessageLayer
from python.Business.BusinessLogicLayer import BusinessLogicLayer
from python.Application.ApplicationLayer import Application
import asyncio
import logging.handlers

logging.basicConfig(
    level=logging.DEBUG,
    format='%(name)s: %(message)s',
)
handler = logging.handlers.RotatingFileHandler(
    "log.txt",
    maxBytes=65536,
    backupCount=10
)

log = logging.getLogger(__name__)

if __name__ == '__main__':
    event_loop = asyncio.get_event_loop()
    mess_to_sock_queue = asyncio.Queue()
    sock_to_mess_queue = asyncio.Queue()
    mess_to_p2p_queue = asyncio.Queue()
    p2p_to_mess_queue  = asyncio.Queue()
    bll_to_p2p_queue = asyncio.Queue()
    p2p_to_bll_queue = asyncio.Queue()

    socket_layer = SocketLayer()
    socket_layer.add_layer_communication(higher=(mess_to_sock_queue, sock_to_mess_queue))

    message_layer = MessageLayer(socket_layer)
    message_layer.add_layer_communication(higher=(p2p_to_mess_queue, mess_to_p2p_queue),
                                          lower=(sock_to_mess_queue, mess_to_sock_queue))

    p2p_layer = P2PLayer(message_layer, bll_to_p2p_queue, p2p_to_bll_queue)
    p2p_layer.add_layer_communication(higher=(bll_to_p2p_queue, p2p_to_bll_queue),
                                      lower=(mess_to_p2p_queue, p2p_to_mess_queue))

    business_logic_layer = BusinessLogicLayer(p2p_layer)
    business_logic_layer.add_layer_communication(lower=(p2p_to_bll_queue, bll_to_p2p_queue))

    app = Application(business_logic_layer)
    app.run()