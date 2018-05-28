package botnet_p2p.kademlia;

import org.junit.Test;

import java.net.UnknownHostException;

import static org.junit.Assert.assertEquals;

public class BucketsListNodeTest {
    @Test
    public void test() throws UnknownHostException {
        KademliaPeer kademliaPeer = new KademliaPeer("127.0.0.1", 8080, 8);

        assertEquals("127.0.0.1", kademliaPeer.getHost().getHostAddress());
        assertEquals(8080, kademliaPeer.getPort());
    }
}