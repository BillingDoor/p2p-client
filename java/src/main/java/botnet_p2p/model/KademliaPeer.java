package botnet_p2p.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class KademliaPeer {
    private String host;
    private int port;
    private String guid;

    public String getId() {
        return guid;
    }
}
