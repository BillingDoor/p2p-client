import python.Message_pb2
import python.peer

def create_find_node_message(sender_id, target_id, address, port):
    """
    Creates protobuf message of FindNode type and returns it as a serialized string of bytes
    :param target_id: ID to find
    :return: String message of bytes
    """
    msg = _prepare_base_message(sender_id, address, port)
    msg.pFindNode.guid = target_id
    msg.type = msg.FIND_NODE
    return msg.SerializeToString()

def create_ping_message(sender_id, address, port):
    """
    Creates protobuf message of Ping type and returns it as a serialized string of bytes
    :param sender_id: ID of source peer
    :return: String message of bytes
    """
    msg = _prepare_base_message(sender_id, address, port)
    msg.type = msg.PING
    return msg.SerializeToString()

def create_find_value_message(sender_id, target_id, address, port):
    """
    Creates protobuf message of FindValue type and returns it as a serialized string of bytes
    :param sender_id: ID of source peer
    :param target_id: ID to find
    :return: String message of bytes
    """
    msg = _prepare_base_message(sender_id, address, port)
    msg.pFindValue.guid = target_id
    msg.type = msg.FIND_VALUE

    return msg.SerializeToString()

def create_found_nodes_message(sender_id, peers, address, port):
    """
    Given list of peers, creates protobuf message of FoundNodes type and returns it as a serialized string of bytes
    :param sender_id: ID of source peer
    :param peers: List of peers to send
    :return: String message of bytes
    """
    msg = _prepare_base_message(sender_id, address, port)
    msg.type = msg.FOUND_NODES
    for peer in peers:
        found_node = msg.pFoundNodes.nodes.add()
        found_node.guid = peer.id
        found_node.IP = peer.host
        found_node.Port = str(peer.port)
        found_node.isNAT = peer.is_NAT

    return msg.SerializeToString()

def _prepare_base_message(id, address, port):
    """
    Prepares base message that is the same in all types of messages
    :param address:
    :param port:
    :param id: id of sender
    :return: Prepared message
    """
    msg = python.Message_pb2.Message()
    msg.sender = address + ":" + str(port)
    msg.uuid = id
    return msg

def read_message(message):
    """
    Takes string containing protobuf message serialized to string of bytes and returns decoded message.
    :param message: String containing serialized message
    :return: Message object
    """
    msg = python.Message_pb2.Message()
    msg.ParseFromString(message)
    return msg

def get_peers_from_found_nodes_message(message):
    """
    Get peers from FOUND_NODES message and return list containing them
    :param message: FOUND_NODES message
    :return: List containing Peers
    """
    return [python.peer.Peer(node.IP, int(node.Port), node.guid, node.isNAT) for node in message.pFoundNodes.nodes]

