import python.Protobuf.Message_pb2
import python.P2P.peer
import random

def create_find_node_message(sender, receiver, guid):
    """
    Creates protobuf message of FIND_NODE type and returns it as a serialized string of bytes
    :param target_id: ID to find
    :return: FIND_NODE Message
    """
    msg = _prepare_base_message(sender, receiver)
    msg.findNode.guid = str(guid)
    msg.type = msg.FIND_NODE
    return msg

def create_ping_message(sender, receiver):
    """
    Creates protobuf message of PING type and returns it as a serialized string of bytes
    :param sender: Sending Peer
    :param receiver: Receiving Peer
    :return: PING Message
    """
    msg = _prepare_base_message(sender, receiver)
    msg.type = msg.PING
    return msg

def create_ping_response_message(sender, receiver):
    """
    Creates protobuf message of PING_RESPONSE type and returns it as a serialized string of bytes
    :param sender: Sending Peer
    :param receiver: Receiving Peer
    :return: PING_RESPONSE Message
    """
    msg = _prepare_base_message(sender, receiver)
    msg.type = msg.PING_RESPONSE
    return msg

def create_command_message(sender, receiver, command, should_respond=False):
    """
    Creates protobuf message of COMMAND type and returns it as a serialized string of bytes
    :param sender: Sending Peer
    :param receiver: Receiving Peer
    :param command: Command to send
    :param should_response: True i
    :return:
    """
    msg = _prepare_base_message(sender, receiver)
    msg.type = msg.COMMAND
    msg.command.command = command
    msg.command.shouldRespond = should_respond
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

def create_found_nodes_message(sender, receiver, nearest_peers):
    """
    Returns new FOUND_NODES message containing nearest_peers informations
    :param sender: Sending Peer
    :param receiver: Receiving Peer
    :param nearest_peers: Nearest peers to include in the message
    :return: FOUND_NODES Message
    """
    msg = _prepare_base_message(sender=sender, receiver=receiver)
    msg.type = msg.FOUND_NODES

    for peer in nearest_peers:
        found_node = msg.foundNodes.nodes.add()
        found_node.guid = str(peer.id)
        found_node.IP = peer.ip
        found_node.port = peer.port
        found_node.isNAT = peer.is_NAT

    return msg

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

def create_peer_from_contact(contact):
    """
    Takes Message.Contact object, gets information from it and returns new Peer object containing that information
    :param contact: Message.Contact object
    :return: new Peer
    """
    id = int(contact.guid)
    ip = contact.IP
    port = contact.port
    is_NAT = contact.isNAT
    return python.P2P.peer.Peer(id, ip, port, is_NAT)

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
    return [create_peer_from_contact(contact) for contact in message.foundNodes.nodes]

