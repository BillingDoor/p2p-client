package botnet_p2p.p2p_layer;

import botnet_p2p.MessageOuterClass;
import botnet_p2p.message_layer.MessageLayer;
import botnet_p2p.model.KademliaPeer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.concurrent.BlockingQueue;

@Configuration
@Import(botnet_p2p.message_layer.Config.class)
public class Config {

    private MessageLayer messageLayer;
    private BlockingQueue<MessageOuterClass.Message> decodedMessages;
    private KademliaPeer me;

    public Config(MessageLayer messageLayer, BlockingQueue<MessageOuterClass.Message> decodedMessages, KademliaPeer me) {
        this.messageLayer = messageLayer;
        this.decodedMessages = decodedMessages;
        this.me = me;
    }

    @Bean
    public P2pLayer p2pLayer() {
        return new P2pLayer(messageLayer, decodedMessages, me);
    }
}
