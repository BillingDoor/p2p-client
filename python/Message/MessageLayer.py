
class MessageLayer:
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

    def ping(self, message):
        """
        Call Socket layer to send given message
        :param message: Bytes of message
        :return: SUCCESS or ERROR
        """
        status = self.socket_layer.send(message)
        return status