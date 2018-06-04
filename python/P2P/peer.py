import random
class Peer(object):
    """
    Peer
    """
    def __init__(self, id, address, port, is_NAT = False):
        self.ip, self.port = address, port
        local_random = random.Random()
        local_random.seed(int(''.join(address.split('.'))) * int(port))
        if id is None:
            self.id = local_random.getrandbits(64)
        else:
            self.id = id
        self.is_NAT = is_NAT

    def __eq__(self, other):
        return self.get_info() == other.get_info()

    def get_info(self):
        return (self.id, self.ip, self.port, self.is_NAT)