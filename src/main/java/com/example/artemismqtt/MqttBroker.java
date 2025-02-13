package com.example.artemismqtt;

import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.core.config.ClusterConnectionConfiguration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.core.server.cluster.impl.MessageLoadBalancingType;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.spi.core.security.ActiveMQBasicSecurityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.*;

@Configuration
public class MqttBroker {

    @Value("${app.number}")
    private String number;

    @Value("${app.size}")
    private String size;

    @Bean
    public EmbeddedActiveMQ embeddedActiveMQ() throws Exception {

        if (number == null) {
            number = "0";
        }
        int num = Integer.parseInt(number);
        if (size == null) {
            size = "1";
        }
        int clusterSize = Integer.parseInt(size);


        EmbeddedActiveMQ embeddedActiveMQ = new EmbeddedActiveMQ();
        org.apache.activemq.artemis.core.config.Configuration config = new ConfigurationImpl();

        //data
        config.setPersistenceEnabled(true);
        config.setJournalDirectory("data" + num + "/journal");
        config.setBindingsDirectory("data" + num + "/bindings");
        config.setPagingDirectory("data" + num + "/paging");
        config.setLargeMessagesDirectory("data" + num + "/largemessages");


        //authentication
        config.setSecurityEnabled(true);
        ActiveMQBasicSecurityManager securityManager = new ActiveMQBasicSecurityManager();
        Map<String, String> properties = new HashMap<>();
        properties.put(ActiveMQBasicSecurityManager.BOOTSTRAP_USER, "admin");
        properties.put(ActiveMQBasicSecurityManager.BOOTSTRAP_PASSWORD, "admin");
        properties.put(ActiveMQBasicSecurityManager.BOOTSTRAP_ROLE, "amq");
        securityManager.init(properties);
        embeddedActiveMQ.setSecurityManager(securityManager);

        config.addAcceptorConfiguration("mqtt", "tcp://0.0.0.0:" + (1083 + num) + "?protocols=MQTT");


        //authorization
        Role adminRole = new Role("amq", true, true, true, true, true, true, true, true, true, true, true, true);
        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        Map<String, Set<Role>> securityRoles = new HashMap<>();
        securityRoles.put("#", roles);
        config.setSecurityRoles(securityRoles);

        //clustering
        List<String> connectorNames = new ArrayList<>();
        config.addAcceptorConfiguration("tcp", "tcp://0.0.0.0:" + (61616 + num) + "?protocols=CORE");
        for (int i = 0; i < clusterSize; i++) {
            Map<String, Object> params = new HashMap<>();
            params.put("host", "localhost");
            params.put("port", 61616 + i);
            TransportConfiguration transportConfig = new TransportConfiguration(NettyConnectorFactory.class.getName(), params);
            config.getConnectorConfigurations().put("node" + i, transportConfig);
            connectorNames.add("node" + i);
        }

        ClusterConnectionConfiguration clusterConnConfig = new ClusterConnectionConfiguration()
                .setName("cluster-connection-node" + num)
                .setConnectorName("node" + num)
                .setStaticConnectors(connectorNames)
                .setRetryInterval(1000)
                .setMessageLoadBalancingType(MessageLoadBalancingType.ON_DEMAND)
                .setDuplicateDetection(false);
        config.getClusterConfigurations().add(clusterConnConfig);

        embeddedActiveMQ.setConfiguration(config);
        embeddedActiveMQ.start();
        return embeddedActiveMQ;
    }
}
