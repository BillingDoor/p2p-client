package botnet_p2p.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Communication<T> {
    private T data;
    private Peer peer;
}
