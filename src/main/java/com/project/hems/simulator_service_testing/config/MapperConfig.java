package com.project.hems.simulator_service_testing.config;

import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.project.hems.simulator_service_testing.domain.MeterEntity;
import com.project.hems.simulator_service_testing.model.VirtualSmartMeter;

@Configuration
public class MapperConfig {

    @Bean
    public ModelMapper getModelMapper() {

        ModelMapper mapMeterEntityTOVirtualSmartMeter = new ModelMapper();

        Converter<MeterEntity, VirtualSmartMeter> converter = ctx -> {
            MeterEntity meterEntity = ctx.getSource();
            return new VirtualSmartMeter(
                    meterEntity.getUserId(),
                    meterEntity.getLastKnownKwh());
        };

        mapMeterEntityTOVirtualSmartMeter.addConverter(converter, MeterEntity.class, VirtualSmartMeter.class);

        return mapMeterEntityTOVirtualSmartMeter;
    }
}
