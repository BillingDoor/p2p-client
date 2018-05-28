package botnet_p2p;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {
    @Bean
    public Botnet botnet() {
        return new Botnet(3000, messageReceiver(), nodeManager());
    }

    @Bean
    public NodeManager nodeManager() {
        return new NodeManager();
    }

    @Bean
    public MessageHandler messageHandler() {
        return new MessageHandler();
    }

    @Bean
    public MessageReceiver messageReceiver() {
        return new MessageReceiver(messageHandler());
    }


}
