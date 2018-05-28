package botnet_p2p.test_node;

import botnet_p2p.MessageHandler;
import botnet_p2p.MessageReceiver;
import botnet_p2p.NodeManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class TestNodeConfig {

    @Bean
    public TestNode testNode() throws IOException, InterruptedException {
        return new TestNode(messageReceiver(), nodeManager());
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
