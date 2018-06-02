import unittest.mock
import python.Protobuf.protobuf_utils as putils
from python.P2P.peer import Peer
from python.Protobuf.Message_pb2 import Message

class ProtobufTest(unittest.TestCase):
    def setUp(self):
        self.sender = Peer(123, '123.123.123.123', 8022, True)
        self.receiver = Peer(11, '66.22.66.22', 9090, False)

    def test_creation_of_basic_message(self):
        sender = self.sender
        receiver = self.receiver
        mess = putils._prepare_base_message(sender, receiver)

        self.assertEqual(mess.sender.guid, '123')
        self.assertEqual(mess.sender.IP, '123.123.123.123')
        self.assertEqual(mess.sender.port, 8022)
        self.assertEqual(mess.sender.isNAT, True)

        self.assertEqual(mess.receiver.guid, '11')
        self.assertEqual(mess.receiver.IP, '66.22.66.22')
        self.assertEqual(mess.receiver.port, 9090)
        self.assertEqual(mess.receiver.isNAT, False)

    def test_creation_of_ping_message(self):
        mess = putils.create_ping_message(self.sender, self.receiver)
        self.assertEqual(mess.type, Message.PING)

    def test_encoding_and_decoding(self):
        mess = putils.create_ping_message(self.sender, self.receiver)
        self.assertEqual(putils.deserialize_message(putils.serialize_message(mess)), mess)

    def test_get_receiver_address(self):
        mess = putils.create_ping_message(self.sender, self.receiver)
        self.assertEqual((self.receiver.ip, self.receiver.port), putils.get_receiver_address(mess))

if __name__ == '__main__':
    unittest.main()
