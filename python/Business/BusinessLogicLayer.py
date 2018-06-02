import python.Protobuf.protobuf_utils as putils
class mock_p2p:
    def join_network(self):
        return True

class BusinessLogicLayer:
    def __init__(self, lower_layer):
        self.lower_layer = lower_layer

    async def add_layer_communication(self, higher=None, lower=None):
        """
        Adds means of communicating with lower and/or lower layer. Higher and lower should be a tuple of two objects
        that support asynchronous communication using get() and put() method to pass along data.
        :param higher: Tuple of two objects for communication with higher layer
        :param lower: Tuple of two objects for communication with lower layer
        """
        if higher:
            self._higher = higher

        if lower:
            self._lower = lower

    async def ping(self, target_id):
        """
        Sends ping message to another peer
        :param target_id: id of target peer
        :return: SUCCESS or ERROR
        """
        sender = await self.lower_layer.get_myself()
        receiver = await self.lower_layer.get_peer_by_id(target_id)
        message = putils.create_ping_message()

    async def join_network(self, bootstrap_node):
        pass