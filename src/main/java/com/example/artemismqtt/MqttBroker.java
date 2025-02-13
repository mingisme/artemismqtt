package com.example.artemismqtt;

import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MqttBroker {

    @Bean
    public EmbeddedActiveMQ embeddedActiveMQ() throws Exception {
        EmbeddedActiveMQ embeddedActiveMQ = new EmbeddedActiveMQ();
        org.apache.activemq.artemis.core.config.Configuration  config = new ConfigurationImpl();
        config.setSecurityEnabled(false);
        config.setPersistenceEnabled(false);
        config.addAcceptorConfiguration("mqtt", "tcp://0.0.0.0:1083?protocols=MQTT");
        embeddedActiveMQ.setConfiguration(config);
        embeddedActiveMQ.start();
        return embeddedActiveMQ;
    }
}
