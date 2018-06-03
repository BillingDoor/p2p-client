package botnet_p2p.socket_layer;

import botnet_p2p.model.KademliaPeer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;


@Configuration
@Import(botnet_p2p.Config.class)
public class Config {
    private BlockingQueue<ByteBuffer> receivedMessages;
    private KademliaPeer me;
    private CountDownLatch initLatch;

    public Config(BlockingQueue<ByteBuffer> receivedMessages, KademliaPeer me, CountDownLatch initLatch) {
        this.receivedMessages = receivedMessages;
        this.me = me;
        this.initLatch = initLatch;
    }

    @Bean
    public SocketLayer socketLayer() throws InterruptedException {
        SocketLayer socketLayer = new SocketLayer(receivedMessages, initLatch, me.getPort());
        socketLayer.start();
        initLatch.await();
        return socketLayer;
    }
}
