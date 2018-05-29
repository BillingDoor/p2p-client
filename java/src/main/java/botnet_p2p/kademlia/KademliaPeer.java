package botnet_p2p.kademlia;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Objects;

public class KademliaPeer {
    private Inet4Address host;
    private int port;
    private long id;

    public KademliaPeer() {
    }

    public KademliaPeer(String host, int port, long id) {
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

    public String getAddress() {
        return host.getHostAddress();
    }

    public int getPort() {
        return port;
    }

    public long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KademliaPeer that = (KademliaPeer) o;
        return port == that.port &&
                id == that.id &&
                Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {

        return Objects.hash(host, port, id);
    }
}
