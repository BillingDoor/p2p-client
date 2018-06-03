package botnet_p2p.model;

import botnet_p2p.MessageOuterClass;
import lombok.Data;

import java.math.BigInteger;
import java.util.Objects;
import java.util.Random;

@Data
public class KademliaPeer {
    private String host;
    private int port;
    private String guid;

    public KademliaPeer(String host, int port, String guid) {
        this.host = host;
        this.port = port;
        this.guid = guid;
    }

    public KademliaPeer(String host, int port) {
        this.host = host;
        this.port = port;
        this.guid = generateGuid();
    }

    // TODO legacy constructor - for tests
    public KademliaPeer(String host, int port, int guid) {
        this.host = host;
        this.port = port;
        this.guid = Integer.toString(guid);
    }

    private String generateGuid() {
        return new BigInteger(64, new Random()).toString();
    }

    @Deprecated
    public String getId() {
        return guid;
    }

    public Peer toPeer() {
        return new Peer(this.host, this.port);
    }

    public static KademliaPeer fromContact(MessageOuterClass.Message.Contact contact) {
        return new KademliaPeer(
                contact.getIP(),
                contact.getPort(),
                contact.getGuid()
        );
    }


    public boolean equalsTo(Peer p) {
        if (p == null ) {
            return false;
        }

        return port == p.getPort() &&
                Objects.equals(host, p.getAddress());
    }

}
