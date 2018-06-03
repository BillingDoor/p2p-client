package botnet_p2p.message_layer;

import botnet_p2p.MessageOuterClass;
import botnet_p2p.socket_layer.SocketLayer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;

@Configuration
@Import(botnet_p2p.socket_layer.Config.class)
public class Config {
    private BlockingQueue<MessageOuterClass.Message> decodedMessages;
    private BlockingQueue<ByteBuffer> receivedMessages;
    private SocketLayer socketLayer;

    public Config(BlockingQueue<MessageOuterClass.Message> decodedMessages, BlockingQueue<ByteBuffer> receivedMessages, SocketLayer socketLayer) {
        this.decodedMessages = decodedMessages;
        this.receivedMessages = receivedMessages;
        this.socketLayer = socketLayer;
    }

    @Bean
    public MessageLayer messageLayer() {
        MessageLayer messageLayer = new MessageLayer(socketLayer, receivedMessages, decodedMessages);
        messageLayer.start();
        return messageLayer;
    }
}
