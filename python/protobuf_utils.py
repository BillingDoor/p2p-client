import python.Message_pb2


def create_find_node_message(ID):
    """
    Creates protobuf message of FindNode type and returns it as a serialized string of bytes
    :param ID: ID to find
    :return: String message of bytes
    """

    msg = python.Message_pb2.Message()
    msg.pFindMode.guid = ID
    msg.type = msg.FIND_NODE

    return msg.SerializeToString()

def read_message(message):
    """
    Takes string containing protobuf message serialized to string of bytes and returns decoded message.
    :param message: String containing serialized message
    :return: Message object
    """
    msg = python.Message_pb2.Message()
    msg.ParseFromString(message)
    return msg
