package com.project.hems.simulator_service_testing.config;

import com.project.hems.simulator_service_testing.model.VirtualSmartMeter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class ActiveMeterMap {

    @Bean
    public Map<Long, VirtualSmartMeter> getActiveMeter(){
       return  new ConcurrentHashMap<>();
    }
}
