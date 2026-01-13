package com.project.hems.simulator_service_testing.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "property.config.kafka")
@Setter
public class KafkaConfig {

    private String topic;

    @Bean
    public NewTopic getTopic() {
        return TopicBuilder.name(topic)
                .partitions(10)
                .replicas(1)
                .build();
    }

}
