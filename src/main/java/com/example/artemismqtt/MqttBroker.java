package com.example.artemismqtt;

import org.apache.activemq.artemis.api.core.BroadcastGroupConfiguration;
import org.apache.activemq.artemis.api.core.DiscoveryGroupConfiguration;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.UDPBroadcastEndpointFactory;
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

    @Bean
    public EmbeddedActiveMQ embeddedActiveMQ() throws Exception {

        if (number == null) {
            number = "0";
        }
        int num = Integer.parseInt(number);


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
        config.addAcceptorConfiguration("mqtt", "tcp://0.0.0.0:"+(1083+num)+"?protocols=MQTT");
        embeddedActiveMQ.setSecurityManager(securityManager);

        //authorization
        Role adminRole = new Role("amq", true, true, true, true, true, true, true, true, true, true, true, true);
        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        Map<String, Set<Role>> securityRoles = new HashMap<>();
        securityRoles.put("#", roles);
        config.setSecurityRoles(securityRoles);

        //clustering
        Map<String, TransportConfiguration> connectors = new HashMap<>();
        TransportConfiguration connectorConfig = new TransportConfiguration(NettyConnectorFactory.class.getName());
        connectors.put("connectorName"+num, connectorConfig);
        config.setConnectorConfigurations(connectors);

        BroadcastGroupConfiguration broadcastGroupConfig = new BroadcastGroupConfiguration()
                .setName("bg-group")
                .setBroadcastPeriod(5000)
                .setConnectorInfos(List.of("connectorName"+num))
                .setEndpointFactory(new UDPBroadcastEndpointFactory()
                        .setGroupAddress("231.7.7.7")
                        .setGroupPort(9876));
        config.setBroadcastGroupConfigurations(Collections.singletonList(broadcastGroupConfig));

        DiscoveryGroupConfiguration discoveryGroupConfig = new DiscoveryGroupConfiguration()
                .setName("dg-group")
                .setRefreshTimeout(10000)
                .setBroadcastEndpointFactory(new UDPBroadcastEndpointFactory()
                        .setGroupAddress("231.7.7.7")
                        .setGroupPort(9876));
        config.setDiscoveryGroupConfigurations(Collections.singletonMap("dg-group1", discoveryGroupConfig));

        ClusterConnectionConfiguration clusterConnectionConfig = new ClusterConnectionConfiguration()
                .setName("cluster-connection")
                .setConnectorName("connectorName"+num)
                .setDiscoveryGroupName("dg-group")
                .setAddress("#")
                .setRetryInterval(1000)
                .setDuplicateDetection(false)
                .setMessageLoadBalancingType(MessageLoadBalancingType.ON_DEMAND);
        config.setClusterConfigurations(Collections.singletonList(clusterConnectionConfig));

        embeddedActiveMQ.setConfiguration(config);
        embeddedActiveMQ.start();
        return embeddedActiveMQ;
    }
}
