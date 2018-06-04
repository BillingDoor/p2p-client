from python.Socket.SocketLayer import SocketLayer
from python.P2P.P2PLayer import P2PLayer, Peer
from python.Message.MessageLayer import MessageLayer
from python.Business.BusinessLogicLayer import BusinessLogicLayer
from python.Application.ApplicationLayer import Application
import asyncio
import sys
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

def _run(cor):
    return asyncio.get_event_loop().run_until_complete(cor)


def run_client(ip, port, bootstrap_node):
    asyncio.set_event_loop(asyncio.new_event_loop())
    mess_to_sock_queue = asyncio.Queue()
    sock_to_mess_queue = asyncio.Queue()
    mess_to_p2p_queue = asyncio.Queue()
    p2p_to_mess_queue = asyncio.Queue()
    bll_to_p2p_queue = asyncio.Queue()
    p2p_to_bll_queue = asyncio.Queue()

    socket_layer = SocketLayer()
    _run(socket_layer.add_layer_communication(higher=(mess_to_sock_queue, sock_to_mess_queue)))

    message_layer = MessageLayer(socket_layer)
    _run(message_layer.add_layer_communication(higher=(p2p_to_mess_queue, mess_to_p2p_queue),
                                               lower=(sock_to_mess_queue, mess_to_sock_queue)))

    p2p_layer = P2PLayer(message_layer, ip, port)
    _run(p2p_layer.add_layer_communication(higher=(bll_to_p2p_queue, p2p_to_bll_queue),
                                           lower=(mess_to_p2p_queue, p2p_to_mess_queue)))

    business_logic_layer = BusinessLogicLayer(p2p_layer)
    _run(business_logic_layer.add_layer_communication(lower=(p2p_to_bll_queue, bll_to_p2p_queue)))
    _run(business_logic_layer.join_network(bootstrap_node))
    _run(asyncio.sleep(25))

    pending = asyncio.Task.all_tasks()
    for task in pending:
        task.cancel()
        asyncio.get_event_loop().run_until_complete(task)
    asyncio.get_event_loop().close()

if __name__ == '__main__':
    ip = sys.argv[1]
    port = int(sys.argv[2])
    if sys.argv[3] != "None":
        bootstrap_node_ip, bootstrap_node_port = sys.argv[3].split(':')
        bootstrap_node = (bootstrap_node_ip, int(bootstrap_node_port))
    else:
        bootstrap_node = None
    run_client(ip, port, bootstrap_node)