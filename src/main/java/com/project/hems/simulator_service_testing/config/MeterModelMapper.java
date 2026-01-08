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
                            .totalEnergyKwh(source.getLastKnownKwh())
                            .userId(source.getUserId())
                            .build();
                });

        mapper.createTypeMap(MeterSnapshot.class, MeterEntity.class)
                .setConverter(ctx -> {
                    MeterSnapshot source = ctx.getSource();

                    MeterEntity entity = new MeterEntity();
                    entity.setId(source.getMeterId());
                    entity.setUserId(source.getUserId());
                    entity.setLastKnownKwh(source.getTotalEnergyKwh());
                    return entity;
                });

        return mapper;
    }
}
