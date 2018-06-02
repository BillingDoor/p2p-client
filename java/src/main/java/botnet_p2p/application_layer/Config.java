package botnet_p2p.application_layer;

import botnet_p2p.business_logic_layer.BusinessLogicLayer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(botnet_p2p.business_logic_layer.Config.class)
public class Config {
    private BusinessLogicLayer businessLogicLayer;

    public Config(BusinessLogicLayer businessLogicLayer) {
        this.businessLogicLayer = businessLogicLayer;
    }

    @Bean
    public ApplicationLayer applicationLayer() {
        return new ApplicationLayer(businessLogicLayer);
    }
}
