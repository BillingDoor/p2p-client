package botnet_p2p.kademlia;

import java.net.Inet4Address;
import java.net.UnknownHostException;

public class KademliaPeer {
    private Inet4Address host;
    private int port;
    private long id;

    public KademliaPeer(String host, int port, int id) {
        this.port = port;
        this.id = id;
        try {
            this.host = (Inet4Address) Inet4Address.getByName(host);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public Inet4Address getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public long getId() {
        return id;
    }
}
