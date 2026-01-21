package com.project.hems.simulator_service_testing.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.project.hems.simulator_service_testing.model.MeterSnapshot;

@Configuration
public class MeterReadingMap {

    @Bean
    public Map<String, MeterSnapshot> getMeterMap() {
        return new ConcurrentHashMap<>();
    }
}
