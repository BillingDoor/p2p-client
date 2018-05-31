package botnet.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Peer {
    private String address;
    private int port;
}
