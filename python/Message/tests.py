import unittest.mock
import asyncio
from python.Socket.SocketLayer import SocketLayer
from python.Message.MessageLayer import MessageLayer
from python.Protobuf.Message_pb2 import Message
import python.Protobuf.protobuf_utils as putils
from python.P2P.peer import Peer

def _run(cor):
    return asyncio.get_event_loop().run_until_complete(cor)

class MessageLayerTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        sl = SocketLayer()
        cls.ml =  MessageLayer(sl)
        q1 = asyncio.Queue()
        q2 = asyncio.Queue()
        q3 = asyncio.Queue()
        q4 = asyncio.Queue()
        cls.higher = (q1, q2)
        cls.lower = (q3, q4)
        _run(cls.ml.add_layer_communication(higher=cls.higher,
                                          lower=cls.lower))
        cls.sender = Peer(123, '123.123.123.123', 8022, True)
        cls.receiver = Peer(11, '66.22.66.22', 9090, False)

    @classmethod
    def tearDownClass(cls):
        pending = asyncio.Task.all_tasks()
        for task in pending:
            task.cancel()
            asyncio.get_event_loop().run_until_complete(task)

    def test_encoding_and_decoding(self):
        mess = putils._prepare_base_message(self.sender, self.receiver)
        serialized = self.ml._serialize_message(mess)
        deserialized = self.ml._deserialize_message(serialized)
        self.assertEqual(deserialized, mess)


    def test_passing_message(self):
        """
        Message layer got some message from higher layer.
        It serializes it.
        Then it passes it on to the lower layer using queue.
        """
        mess = putils.create_ping_message(self.sender, self.receiver)
        address = putils.get_receiver_address(mess)
        _run(self.higher[0].put(mess))
        # wait a while for message to propagate through the layer
        _run(asyncio.sleep(0.1))
        self.assertEqual(_run(self.lower[1].get()), (putils.serialize_message(mess), address))
