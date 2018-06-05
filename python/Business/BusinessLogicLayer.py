import python.Protobuf.protobuf_utils as putils
import asyncio
import logging.handlers
from python.utils.StatusMessage import StatusMessage
from python.Protobuf.Message_pb2 import Message
from python.P2P.peer import Peer
import os
import subprocess
import python.Business.util as file_util
import random


logging.basicConfig(
    level=logging.DEBUG,
    format='%(name)s: %(message)s',
)
handler = logging.handlers.RotatingFileHandler(
    os.path.abspath("./logs/log.txt"),
)
log = logging.getLogger(__name__)
log.addHandler(handler)
formatter = logging.Formatter('%(name)s: %(message)s')
handler.formatter = formatter
log.propagate = False

class BusinessLogicLayer:
    def __init__(self, lower_layer):
        self.lower_layer = lower_layer
        self._this_peer = lower_layer.get_myself()
        self._pinged_peers = []
        self._files_being_written = []

    async def add_layer_communication(self, lower):
        """
        Adds means of communicating with lower and/or lower layer. Higher and lower should be a tuple of two objects
        that support asynchronous communication using get() and put() method to pass along data.
        Then starts listening on them for data.
        :param higher: Tuple of two objects for communication with higher layer
        :param lower: Tuple of two objects for communication with lower layer
        """
        self._lower = lower
        asyncio.ensure_future(self._handle_lower_input())

    async def command(self, target_id, command, should_respond):
        """
        Sends command to target peer
        :param target_id: id of targeted peer
        :param command: command to send
        :param should_response: True if receiver should respond to the command
        :return:SUCCESS or FAILURE
        """
        peer = await self.lower_layer.get_peer_by_id(target_id)
        if peer is None:
            return StatusMessage.FAILURE
        message = putils.create_command_message(sender=self.get_myself(),
                                                receiver=peer,
                                                command=command,
                                                should_respond=should_respond
                                                )
        try:
            status = await self._put_message_on_lower(message)
            return status
        except asyncio.CancelledError:
            return StatusMessage.FAILURE

    async def ping(self, target_id):
        """
        Sends ping message to peer with given target_id
        :param target_id: id of target peer
        :return: SUCCESS or FAILURE
        """
        peer = await self.lower_layer.get_peer_by_id(target_id)
        if peer is None:
            return StatusMessage.FAILURE
        message = putils.create_ping_message(sender=self.get_myself(), receiver=peer)
        try:
            status = await self._put_message_on_lower(message)
            self._pinged_peers.append((peer, asyncio.ensure_future(self._wait_for_ping_response(peer=peer, timeout=10))))
            return status
        except asyncio.CancelledError:
            return StatusMessage.FAILURE

    async def file_request(self, target_id, filepath):
        peer = await self.lower_layer.get_peer_by_id(target_id)
        if peer is None:
            return StatusMessage.FAILURE
        message = putils.create_file_request_message(sender=self.get_myself(), receiver=peer, path=filepath)
        try:
            status = await self._put_message_on_lower(message)
            return status
        except asyncio.CancelledError:
            return StatusMessage.FAILURE

    def get_myself(self):
        return self._this_peer

    async def find_node(self, guid, id_of_peer_to_ask):
        """
        Create find_node message with given guid to find and peer_to_ask as a receiver of our query and pass it on
        :param guid: id of peer to ask
        :param id_of_peer_to_ask: peer to query about wanted peer
        :return: SUCCESS or FAILURE
        """
        peer_to_ask = await self.lower_layer.get_peer_by_id(id_of_peer_to_ask)
        if peer_to_ask is None:
            return StatusMessage.FAILURE
        message = putils.create_find_node_message(sender=self.get_myself(), receiver=peer_to_ask, guid=guid)
        status = await self._put_message_on_lower(message)
        return status

    async def join_network(self, bootstrap_node):
        """
        Join network that the bootstrap_node belongs to
        :param bootstrap_node: Bootstrap node we will be asking for information about network
        :return: SUCCESS or FAILURE
        """
        sever_status = await self.start_server()
        if sever_status is StatusMessage.FAILURE:
            print("Cancel network joining, could not start the server")
            return sever_status

        if bootstrap_node:
            log.debug("Joining network, bootstrap node: {}".format(bootstrap_node))
            peer_to_ask = Peer(None, bootstrap_node[0], bootstrap_node[1], False)
            await self.lower_layer.add_peer(peer_to_ask)
            await self.ping(peer_to_ask.id)
            log.debug("Waiting for boot node to respond")
            await asyncio.sleep(4)

            peer_to_ask = await self.lower_layer.get_peer_by_id(peer_to_ask.id)
            if peer_to_ask is None:
                log.warning("Bootstrap node is not responding. Failed to bootstrap")
                await self.stop_server()
                return StatusMessage.FAILURE

            message = putils.create_find_node_message(sender=self.get_myself(), receiver=peer_to_ask, guid=self.get_myself().id)
            status = await self._put_message_on_lower(message)
            if status is StatusMessage.FAILURE:
                log.warning("Could not send find node message to bootstrap node")
                await self.stop_server()
                return status
        return StatusMessage.SUCCESS

    async def start_server(self):
        """
        Try to start the server
        """
        return await self.lower_layer.start_server()

    async def stop_server(self):
        """
        Try to stop the server
        """
        await self.lower_layer.stop_server()

    async def get_routing_table_info(self):
        """
        Returns routing table in printable form
        """
        routing_table_info = await self.lower_layer.get_routing_table_info()
        return routing_table_info

    async def _handle_lower_input(self):
        try:
            while True:
                log.debug("Waiting for message from lower layer")
                message = await self._lower[0].get()
                log.debug("Got message {!r}; handle it".format(message))
                await self._handle_message(message)
                log.debug("Message {!r} handled".format(message))
        except asyncio.CancelledError:
            log.debug("Caught CancelledError: Stop handling input from lower layer")

    async def _handle_message(self, message):
        """
        Checks type of message and handle it appropriately
        :param message: Message to handle
        """
        if message.type == Message.PING:
            await self._handle_ping_message(message)
        elif message.type == Message.PING_RESPONSE:
            await self._handle_ping_response_message(message)
        elif message.type == Message.FIND_NODE:
            await self._handle_find_node_message(message)
        elif message.type == Message.FOUND_NODES:
            await self._handle_found_nodes_message(message)
        elif message.type == Message.COMMAND:
            await self._handle_command_message(message)
        elif message.type == Message.COMMAND_RESPONSE:
            await self._handle_command_response_message(message)
        elif message.type == Message.FILE_REQUEST:
            await self._handle_file_request_message(message)
        elif message.type == Message.FILE_CHUNK:
            await self._handle_file_chunk_message(message)
        else:
            log.warning("Unsupported message type {}".format(message.type))

        if message.propagate == True:
            await self._propagate_message(message=message)

    async def _propagate_message(self, message):
        for new_receiver in self.lower_layer._routing_table:
            if new_receiver.id != int(message.receiver.id):
                putils.swap_receiver(message=message, new_receiver=new_receiver)
                await self._put_message_on_lower(message)

    async def _handle_file_request_message(self, message):
        log.debug("Handling FILE_REQUEST message")
        sender = message.sender
        sender_peer = putils.create_peer_from_contact(sender)
        path = message.fileRequest.path
        log.debug("Peer {} is requesting file {}".format(sender_peer.get_info(), path))

        uuid = str(random.Random().getrandbits(64))
        ordinal = 0
        file_size = file_util.get_file_size(path)
        file_name = path + ".{}".format(self.get_myself().id)
        log.debug("Start creating file chunks and sending them in messages")
        for chunk in file_util.chunks_generator(path=path):
            log.debug("Send file chunk: [ {}, {}, {}, {}, data_chunk_size: {} ]".format(uuid, file_name, file_size, ordinal, len(chunk)))
            status = await self._file_chunk_message(receiver=sender_peer,
                                                    uuid=uuid,
                                                    file_name=file_name,
                                                    file_size = file_size,
                                                    ordinal=ordinal,
                                                    data=chunk
                                                    )
            ordinal += 1
            if status is StatusMessage.FAILURE:
                log.warning("Could not create file chunk message")
                return status
            elif status is StatusMessage.SUCCESS:
                continue
        log.debug("Whole file was sent")
        return StatusMessage.SUCCESS

    async def _handle_file_chunk_message(self, message):
        """
        Handle FILE_CHUNK Message
        :param message: message
        :return: SUCCESS or FAILURE
        """
        log.debug("Handling FILE_CHUNK message")
        uuid = message.fileChunk.uuid
        file_name = message.fileChunk.fileName
        file_size = message.fileChunk.fileSize
        ordinal = message.fileChunk.ordinal
        data = message.fileChunk.data

        # We are currently writing this file
        for file_being_written_uuid in self._files_being_written:
            if file_being_written_uuid == uuid:
                log.debug("Adding another chunk to file {}".format(file_name))
                with open(file_name, 'ab') as file:
                    file.seek(ordinal * 8192, 0)
                    file.write(data)
                    log.debug("Added another chunk to file {}".format(file_name))

                if file_size == file_util.get_file_size(path=file_name):
                    log.debug("Whole file {} has been writte".format(file_name))
                    self._files_being_written.remove(file_being_written_uuid)
                return StatusMessage.SUCCESS

        # That file was not being written
        self._files_being_written.append(uuid)
        log.debug("Adding first chunk to file {}".format(file_name))
        with open(file_name, 'wb') as file:
            file.seek(ordinal * 8192, 0)
            file.write(data)
            log.debug("Added first chunk to file {}".format(file_name))

        if file_size == file_util.get_file_size(path=file_name):
            log.debug("Whole file {} has been writte".format(file_name))

            self._files_being_written.remove(uuid)
        return StatusMessage.SUCCESS

    async def _file_chunk_message(self, receiver, uuid, file_name, file_size, ordinal, data):
        message = putils.create_file_chunk_message(sender=self.get_myself(),
                                                   receiver=receiver,
                                                   uuid=uuid,
                                                   file_name=file_name,
                                                   file_size=file_size,
                                                   ordinal=ordinal,
                                                   data=data)
        status = await self._put_message_on_lower(message)
        return status

    async def _handle_command_message(self, message):
        log.debug("Handling COMMAND message")
        sender = message.sender
        command = message.command.command
        should_respond = message.command.shouldRespond
        sender_peer = putils.create_peer_from_contact(sender)

        log.debug("COMMAND message was sent from {}".format(sender_peer.get_info()))
        log.debug("COMMAND to run: {}".format(command))
        try:
            value = subprocess.check_output(command.split(), shell=True).decode('utf-8', 'ignore')

            if should_respond:
                mess_status = await self._command_response(receiver=sender_peer, command=command, value=value, status=0)
                if mess_status is StatusMessage.SUCCESS:
                    log.debug("Command was responded to correctly")
                elif mess_status is StatusMessage.FAILURE:
                    log.warning("Command wasn't responded to correctly")

        except subprocess.CalledProcessError as error:
            result = error.returncode
            log.warning("Could not call command {}, return code: {}".format(command, result))

            if should_respond:
                mess_status = await self._command_response(receiver=sender_peer, command=command, value="", status=result)
                if mess_status is StatusMessage.SUCCESS:
                    log.debug("Command was responded to correctly")
                elif mess_status is StatusMessage.FAILURE:
                    log.warning("Command wasn't responded to correctly")

    async def _handle_command_response_message(self, message):
        log.debug("Handling COMMAND_RESPONSE message")
        sender = message.sender
        sender_peer = putils.create_peer_from_contact(sender)
        command = message.response.command
        value = message.response.value
        status = message.response.status
        print("="*30)
        print("COMMAND: {}".format(command))
        print("RESPONSE: {}".format(value))
        print("="*30)

        log.debug("COMMAND RESPONSE message was sent from {}".format(sender_peer.get_info()))
        log.debug("COMMAND {} returned {} and status value {}".format(command, value, status))

    async def _handle_ping_message(self, message):
        log.debug("Handling PING message")
        sender = message.sender
        sender_peer = putils.create_peer_from_contact(sender)
        log.debug("PING message was sent from {}".format(sender_peer.get_info()))
        log.debug("Adding sender to routing table")
        await self.lower_layer.add_peer(sender_peer)

        log.debug("Send ping response to that peer")
        status = await self._ping_response(sender_peer)
        if status is StatusMessage.SUCCESS:
            log.debug("Ping was responded to correctly")
        else:
            log.warning("Ping wasn't responded to correctly")

    async def _handle_ping_response_message(self, message):
        log.debug("Handling PING_RESPONSE message")
        sender = message.sender
        sender_peer = putils.create_peer_from_contact(sender)
        log.debug("PING_RESPONSE message was sent from {}".format(sender_peer.get_info()))
        log.debug("Remove that peer from list of peers to remove if they are not responsive after timeout")

        for pinged_peer in self._pinged_peers:
            if pinged_peer[0] == sender_peer:
                pinged_peer[1].cancel()
                log.debug("Cancelled removal of responsive peer")
                self._pinged_peers.remove(pinged_peer)
                break
        log.debug("PING_RESPONSE message was handled")

    async def _handle_found_nodes_message(self, message):
        """
        Handles FOUND_NODES message
        :param message: message to handle
        """
        log.debug("Handling FOUND_NODES message")
        sender = message.sender
        sender_peer = putils.create_peer_from_contact(sender)
        log.debug("FOUND_NODES message was sent from {}".format(sender_peer.get_info()))
        peers = putils.get_peers_from_found_nodes_message(message)
        for peer in peers:
            if peer.id == self.get_myself().id:
                continue
            await self.lower_layer.add_peer(peer)
            await self.ping(peer.id)

    async def _handle_find_node_message(self, message):
        """
        Handles find node message
        :param message:
        :return:
        """
        log.debug("Handling FIND_NODE message")
        sender = message.sender
        guid = int(message.findNode.guid)
        sender_peer = putils.create_peer_from_contact(sender)
        log.debug("FIND_NODE message was sent from {}".format(sender_peer.get_info()))
        log.debug("Adding sender to routing table")
        await self.lower_layer.add_peer(sender_peer)

        log.debug("Send FOUND NODE message to that peer")
        await self._found_nodes_message(receiver=sender_peer, wanted_peer_id=guid)

    async def _found_nodes_message(self, receiver, wanted_peer_id):
        """
        Sends FOUND NODE message to target peer
        :param receiver: Peer to send the message to
        :param wanted_peer_id: id of peer that is wanted
        :return: SUCCESS or FAILURE
        """
        peers = await self.lower_layer.get_nearest_peers(wanted_peer_id=wanted_peer_id)
        if receiver in peers:
            peers.remove(receiver)
        message = putils.create_found_nodes_message(sender=self.get_myself(), receiver=receiver, nearest_peers=peers)
        status = await self._put_message_on_lower(message)
        return status

    async def _command_response(self, receiver, command, value, status):
        """
        Sends command response message with given value and status code
        :param receiver: Peer that should receive the message
        :param value: Value that was returned after calling the command
        :param status: Status value
        :return: SUCCESS or FAILURE
        """
        message = putils.create_command_response_message(sender=self.get_myself(),
                                                                receiver=receiver,
                                                                command=command,
                                                                value=value,
                                                                status=status)
        send_status = await self._put_message_on_lower(message)
        return send_status

    async def _ping_response(self, receiver):
        """
        Sends ping response message to target peer
        :param receiver: Peer to send the message to
        :return: SUCCESS or FAILURE
        """
        message = putils.create_ping_response_message(sender=self.get_myself(), receiver=receiver)
        status = await self._put_message_on_lower(message)
        return status

    async def _put_message_on_lower(self, message):
        """
        Puts message on lower writing queue
        :param message: message to put
        :return: SUCCESS or FAILURE
        """
        log.debug("Putting message to Queue")
        try:
            await self._lower[1].put(message)
            log.debug("Message {} put into Queue".format(message))
            return StatusMessage.SUCCESS
        except asyncio.CancelledError:
            log.debug("Message {} has not been put onto {} because CancelledError was caught".format(
                message,
                self._lower[1]
            ))
            return StatusMessage.FAILURE

    async def _wait_for_ping_response(self, peer, timeout):
        """
        Wait for ping response from peer and if it doesn't come then remove it from the routing table
        :param peer:
        :return:
        """
        try:
            await asyncio.sleep(timeout)
            log.debug("Peer {} has not responded in {} second. Remove him".format(peer.get_info(), timeout))
            await self.lower_layer.remove_peer(peer)
            log.debug("Peer {} removed".format(peer.get_info()))

        except asyncio.CancelledError:
            log.debug("Peer {} responded. Cancel removal of him from the routing table.")


