import unittest.mock
import asyncio
import socket
from python.Socket.SocketLayer import SocketLayer
import python.Protobuf.protobuf_utils as putils
from python.P2P.peer import Peer
import logging
import sys
from python.StatusMessage import StatusMessage

logging.basicConfig(format="%(name)s %(message)s", stream=sys.stderr, level=logging.DEBUG)
log = logging.getLogger('Socket-tests')

def _run(cor):
    return asyncio.get_event_loop().run_until_complete(cor)

class SocketsTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.socket_layer = SocketLayer()
        q1 = asyncio.Queue()
        q2 = asyncio.Queue()
        cls.higher = (q1, q2)
        _run(cls.socket_layer.add_layer_communication(higher=cls.higher))
        cls.sender = Peer(123, '123.123.123.123', 8022, True)
        cls.receiver = Peer(11, '66.22.66.22', 9090, False)

    @classmethod
    def tearDownClass(cls):
        pending = asyncio.Task.all_tasks()
        for task in pending:
            task.cancel()
            asyncio.get_event_loop().run_until_complete(task)

    def test_starting_server(self):
        """
        We tell SocketLayer to start the server.
        It should start it in another thread and listen for incoming connections at given socket.
        We try to connect with it and if successful we close the connection
        Then we tell it to stop running
        """
        self.socket_layer.start_server("127.0.0.1", 8080)
        log.debug("Create socket")
        sock = socket.socket()
        try:
            sock.connect(("127.0.0.1", 8080))
        except socket.error as msg:
            log.warning("Could not connect to the server")
            return

        sock.close()

        #Get the message
        log.warning("Stop the server")
        status = _run(self.socket_layer.stop_server())
        self.assertIs(status, StatusMessage.SUCCESS)
        # And now we check if the message is correct

    def test_that_server_receives_messages(self):
        """
        We tell SocketLayer to start the server.
        It should start it in another thread and listen for incoming connections at given socket.
        We test that by sending some message to the server and if everything works it should handle it and put it into the queue.
        """
        self.socket_layer.start_server("127.0.0.1", 8080)
        log.debug("Create socket")
        sock = socket.socket()
        try:
            sock.connect(("127.0.0.1", 8080))
        except socket.error as msg:
            log.warning("Could not connect to the server")
            return

        mess = putils.create_ping_message(self.sender, self.receiver)
        serialized = putils.serialize_message(mess)

        try:
            log.debug("Try to send the message")
            sock.send(serialized)
        except socket.error as msg:
            log.warning("Could not send the message: {}".format(msg))
            sock.close()
            return

        sock.close()

        #Get the message
        msg = _run(self.higher[1].get())
        log.warning("Stop the server")
        status = _run(self.socket_layer.stop_server())
        self.assertIs(status, StatusMessage.SUCCESS)
        # And now we check if the message is correct
        self.assertEqual(serialized, msg)



if __name__ == '__main__':
    unittest.main()