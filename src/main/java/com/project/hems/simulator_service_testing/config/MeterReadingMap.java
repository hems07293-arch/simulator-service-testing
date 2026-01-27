package com.project.hems.simulator_service_testing.config;

import com.project.hems.simulator_service_testing.model.MeterSnapshot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class MeterReadingMap {

    @Bean
    public Map<String, MeterSnapshot> getMeterMap() {
        return new ConcurrentHashMap<>();
    }
}
