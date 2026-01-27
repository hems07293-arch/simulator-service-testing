package com.project.hems.simulator_service_testing.config;

import lombok.Setter;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConfigurationProperties(prefix = "property.config.kafka")
@Setter
public class KafkaConfig {

    private String rawEnergyTopic;
    private String siteCreationTopic;
    private Integer rawEnergyPartitionCount;
    private Integer siteCreationPartitionCount;
    private Integer replicaCount;

    @Bean
    public NewTopic rawEnergyReadings() {
        return TopicBuilder.name(rawEnergyTopic)
                .partitions(rawEnergyPartitionCount)
                .replicas(replicaCount)
                .build();
    }


    @Bean
    public NewTopic siteCreationTopic(){
        return TopicBuilder.name(siteCreationTopic)
                .partitions(siteCreationPartitionCount)
                .replicas(replicaCount)
                .build();

    }

}
