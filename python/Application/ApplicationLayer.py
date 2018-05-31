from python.Socket.SocketLayer import SocketLayer
from python.Protobuf.protobuf_utils import ProtobufLayer
from python.P2P.P2PLayer import P2PLayer
from python.Message.MessageLayer import MessageLayer
from python.Business.BusinessLogicLayer import BusinessLogicLayer


class Application:
    def __init__(self, business_logic_layer):
        self.business_logic_layer = business_logic_layer


if __name__ == '__main__':
    socket_layer = SocketLayer()
    message_layer = MessageLayer(socket_layer)
    p2p_layer = P2PLayer(message_layer)
    business_logic_layer = BusinessLogicLayer(p2p_layer)
    app = Application(business_logic_layer)
    app.run()


