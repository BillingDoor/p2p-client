import python.Protobuf.Message_pb2
import python.P2P.peer
import random

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

def create_ping_message(sender, receiver):
    """
    Creates protobuf message of Ping type and returns it as a serialized string of bytes
    :param sender: Sending Peer
    :param receiver: Receiving Peer
    :return: Ping Message
    """
    msg = _prepare_base_message(sender, receiver)
    msg.type = msg.PING
    return msg

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

def _prepare_base_message(sender, receiver):
    """
    Prepare base for all other messages
    :param sender: Peer of sender
    :param receiver: Peer of receiver
    :return: Message
    """
    msg = python.Protobuf.Message_pb2.Message()
    msg.sender.guid = str(sender.id)
    msg.sender.IP = sender.ip
    msg.sender.port = sender.port
    msg.sender.isNAT = sender.is_NAT

    msg.receiver.guid = str(receiver.id)
    msg.receiver.IP = receiver.ip
    msg.receiver.port = receiver.port
    msg.receiver.isNAT = receiver.is_NAT

    msg.uuid = str(random.Random().getrandbits(32))
    return msg

def deserialize_message(message):
    """
    Takes string containing protobuf message serialized to string of bytes and returns deserialized message.
    :param message: String containing serialized message
    :return: Message object
    """
    msg = python.Protobuf.Message_pb2.Message()
    msg.ParseFromString(message)
    return msg

def serialize_message(message):
    """
    Takes instance of Message class and serializes it to string of bytes.
    :param message: Message to serialize
    :return: Serialized message
    """
    return message.SerializeToString()

def get_receiver_address(message):
    """
    Returns receiver address from message
    :param message: message to get info from
    :return: address: (ip, port)
    """
    ip = message.receiver.IP
    port = message.receiver.port
    return ip, port


def get_peers_from_found_nodes_message(message):
    """
    Get peers from FOUND_NODES message and return list containing them
    :param message: FOUND_NODES message
    :return: List containing Peers
    """
    return [python.P2P.peer.Peer(node.IP, int(node.Port), node.guid, node.isNAT) for node in message.pFoundNodes.nodes]

