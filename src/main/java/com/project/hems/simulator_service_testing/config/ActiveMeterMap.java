package com.project.hems.simulator_service_testing.config;

import com.project.hems.simulator_service_testing.model.VirtualSmartMeter;
import com.project.hems.simulator_service_testing.repository.MeterRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ActiveMeterMap {

    private final MeterRepository meterRepository;

    private final ModelMapper mapper;

    @Bean
    public Map<Long, VirtualSmartMeter> getActiveMeter() {
        log.info("fetching all meter readings from db");

        Map<Long, VirtualSmartMeter> meterReadingOfUser = new ConcurrentHashMap<>();
        meterRepository.findAll()
                .forEach(meterEntity -> {
                    meterReadingOfUser.put(meterEntity.getId(), mapper.map(meterEntity, VirtualSmartMeter.class));
                });
        return meterReadingOfUser;
    }
}
