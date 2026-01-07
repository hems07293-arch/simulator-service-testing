package com.project.hems.simulator_service_testing.config;

import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.project.hems.simulator_service_testing.domain.MeterEntity;
import com.project.hems.simulator_service_testing.model.MeterSnapshot;

@Configuration
public class MapperConfig {

    @Bean
    public ModelMapper getModelMapper() {

        ModelMapper mapMeterEntityTOMeterSnapshot = new ModelMapper();

        Converter<MeterEntity, MeterSnapshot> converter = ctx -> {
            MeterEntity meterEntity = ctx.getSource();
            return MeterSnapshot
                    .builder()
                    .userId(meterEntity.getUserId())
                    .totalEnergyKwh(meterEntity.getLastKnownKwh())
                    .build();
        };

        mapMeterEntityTOMeterSnapshot.addConverter(converter, MeterEntity.class, MeterSnapshot.class);

        return mapMeterEntityTOMeterSnapshot;
    }
}
