package com.hazelcast.guide.config;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.session.HazelcastIndexedSessionRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.FlushMode;
import org.springframework.session.SaveMode;
import org.springframework.session.config.SessionRepositoryCustomizer;
import com.hazelcast.spring.session.config.annotation.web.http.EnableHazelcastHttpSession;

import java.time.Duration;

import static com.hazelcast.spring.session.HazelcastSessionConfiguration.applySerializationConfig;

@Configuration
@EnableHazelcastHttpSession
class SessionConfiguration {

    private final String SESSIONS_MAP_NAME = "spring-session-map-name";

    //tag::customization[]
    @Bean
    public SessionRepositoryCustomizer<HazelcastIndexedSessionRepository> customize() {
        return (sessionRepository) -> {
            sessionRepository.setFlushMode(FlushMode.IMMEDIATE);
            sessionRepository.setSaveMode(SaveMode.ALWAYS);
            sessionRepository.setSessionMapName(SESSIONS_MAP_NAME);
            sessionRepository.setDefaultMaxInactiveInterval(Duration.ofMinutes(2));
        };
    }
    //end::customization[]

    //tag::hazelcastInstance[]
    @Bean
    @ConditionalOnExpression("${guide.useClientServer:false} == false")
    public HazelcastInstance hazelcastInstance() {
        Config config = new Config();
        config.getNetworkConfig().getJoin().getTcpIpConfig()
              .setEnabled(true)
              .addMember("127.0.0.1");

        return Hazelcast.newHazelcastInstance(applySerializationConfig(config));
    }
    //end::hazelcastInstance[]

    //tag::hazelcastClient[]
    @Bean
    @ConditionalOnExpression("${guide.useClientServer:false}")
    public HazelcastInstance hazelcastClient() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getNetworkConfig().addAddress("127.0.0.1:5701");
        return HazelcastClient.newHazelcastClient(applySerializationConfig(clientConfig));
    }
    //end::hazelcastClient[]

}
