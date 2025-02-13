package com.example.artemismqtt;

import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.spi.core.security.ActiveMQBasicSecurityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Configuration
public class MqttBroker {

    @Bean
    public EmbeddedActiveMQ embeddedActiveMQ() throws Exception {
        EmbeddedActiveMQ embeddedActiveMQ = new EmbeddedActiveMQ();
        org.apache.activemq.artemis.core.config.Configuration  config = new ConfigurationImpl();

        //authentication
        config.setSecurityEnabled(true);
        ActiveMQBasicSecurityManager securityManager = new ActiveMQBasicSecurityManager();
        Map<String,String> properties = new HashMap<>();
        properties.put(ActiveMQBasicSecurityManager.BOOTSTRAP_USER, "admin");
        properties.put(ActiveMQBasicSecurityManager.BOOTSTRAP_PASSWORD, "admin");
        properties.put(ActiveMQBasicSecurityManager.BOOTSTRAP_ROLE, "amq");
        securityManager.init(properties);
        config.addAcceptorConfiguration("mqtt", "tcp://0.0.0.0:1083?protocols=MQTT");
        embeddedActiveMQ.setSecurityManager(securityManager);

        //authorization
        Role adminRole = new Role("amq", true, true, true, true, true, true, true, true, true, true, true, true);
        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        Map<String, Set<Role>> securityRoles = new HashMap<>();
        securityRoles.put("#", roles);
        config.setSecurityRoles(securityRoles);

        embeddedActiveMQ.setConfiguration(config);
        embeddedActiveMQ.start();
        return embeddedActiveMQ;
    }
}
