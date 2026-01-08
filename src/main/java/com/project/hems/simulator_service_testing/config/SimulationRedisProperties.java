package com.project.hems.simulator_service_testing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "simulation-redis.config")
public class SimulationRedisProperties {

    private String REDIS_KEY;

}

