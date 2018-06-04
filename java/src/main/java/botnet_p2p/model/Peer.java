package botnet_p2p.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
@Getter
@AllArgsConstructor
public class Peer {
    private String address;
    private int port;
}
