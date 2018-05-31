package botnet.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Communication<T> {
    private T data;
    private Peer peer;
}
