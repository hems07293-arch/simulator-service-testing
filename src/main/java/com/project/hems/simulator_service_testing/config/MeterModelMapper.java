package com.project.hems.simulator_service_testing.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.project.hems.simulator_service_testing.domain.MeterEntity;
import com.project.hems.simulator_service_testing.model.MeterSnapshot;

@Configuration
public class MeterModelMapper {

    @Bean
    public ModelMapper getModelMapper() {
        ModelMapper mapper = new ModelMapper();

        mapper.getConfiguration()
                .setImplicitMappingEnabled(false)
                .setAmbiguityIgnored(true);

        mapper.createTypeMap(MeterEntity.class, MeterSnapshot.class)
                .setConverter(ctx -> {
                    MeterEntity source = ctx.getSource();

                    return MeterSnapshot.builder()
                            .meterId(source.getId())
                            .userId(source.getUserId())
                            .totalEnergyKwh(source.getLastKnownEnergyKwh())
                            .chargingStatus(source.getChargingStatus())
                            .batteryCapacityWh(source.getBatteryCapacityWh())
                            .batteryRemainingWh(source.getBatteryRemainingWh())
                            .build();
                });

        mapper.createTypeMap(MeterSnapshot.class, MeterEntity.class)
                .setConverter(ctx -> {
                    MeterSnapshot source = ctx.getSource();

                    MeterEntity entity = new MeterEntity();
                    entity.setId(source.getMeterId());
                    entity.setUserId(source.getUserId());
                    entity.setLastKnownEnergyKwh(source.getTotalEnergyKwh());
                    entity.setChargingStatus(source.getChargingStatus());
                    entity.setBatteryCapacityWh(source.getBatteryCapacityWh());
                    entity.setBatteryRemainingWh(source.getBatteryRemainingWh());
                    entity.setBatterySoc(source.getBatterySoc());
                    return entity;
                });

        return mapper;
    }
}
