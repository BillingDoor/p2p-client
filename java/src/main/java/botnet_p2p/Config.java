package botnet_p2p;

import botnet_p2p.model.KademliaPeer;
import botnet_p2p.model.Peer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;


@Configuration
@PropertySource("application.properties")
public class Config {

    @Bean
    public KademliaPeer me(@Value("${me.host}") String host, @Value("${me.port}") int port) {
        return new KademliaPeer(host, port); // me
    }

    @Bean(name = "bootstrap")
    public Peer bootstrapNode(@Value("${boot.host}") String host, @Value("${boot.port}") int port) {
        return new Peer(host, port);
    }

    @Bean
    public BlockingQueue<ByteBuffer> receivedMessages() {
        return new LinkedBlockingQueue<>(); // coming from the world
    }

    @Bean
    public BlockingQueue<MessageOuterClass.Message> decodedMessages() {
        return new LinkedBlockingQueue<>(); // coming from the world, decoded
    }

    @Bean
    public CountDownLatch initLatch() {
        return new CountDownLatch(1);
    }


}
