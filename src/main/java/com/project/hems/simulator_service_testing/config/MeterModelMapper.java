package com.project.hems.simulator_service_testing.config;

import java.sql.Timestamp;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.project.hems.simulator_service_testing.domain.MeterEntity;
import com.project.hems.simulator_service_testing.model.BatteryMode;
import com.project.hems.simulator_service_testing.model.MeterSnapshot;

@Configuration
public class MeterModelMapper {

    @Bean
    public ModelMapper getModelMapper() {
        ModelMapper mapper = new ModelMapper();

        mapper.getConfiguration()
                .setImplicitMappingEnabled(false)
                .setAmbiguityIgnored(true);

        // 1. Entity -> Snapshot (Used when loading from DB into Redis)
        mapper.createTypeMap(MeterEntity.class, MeterSnapshot.class)
                .setConverter(ctx -> {
                    MeterEntity source = ctx.getSource();

                    return MeterSnapshot.builder()
                            .meterId(source.getId())
                            .siteId(source.getSiteId())
                            // Map cumulative energy values for "Self-Healing" logic
                            .totalGridImportKwh(source.getTotalGridImportKwh())
                            .totalGridExportKwh(source.getTotalGridExportKwh())
                            .totalSolarYieldKwh(source.getTotalSolarYieldKwh())
                            .totalHomeUsageKwh(source.getTotalHomeUsageKwh())
                            // Battery state
                            .chargingStatus(source.getChargingStatus())
                            .batteryCapacityWh(source.getBatteryCapacityWh())
                            .batteryRemainingWh(source.getBatteryRemainingWh())
                            // Default Mode on Load
                            .batteryMode(BatteryMode.AUTO)
                            .batterySoc(source.getBatterySoc())
                            .timestamp(source.getLastUpdatedAt().toLocalDateTime())
                            .build();
                });

        // 2. Snapshot -> Entity (Used when saving Redis state to DB for long-term
        // storage)
        mapper.createTypeMap(MeterSnapshot.class, MeterEntity.class)
                .setConverter(ctx -> {
                    MeterSnapshot source = ctx.getSource();

                    MeterEntity entity = new MeterEntity();
                    entity.setId(source.getMeterId());
                    entity.setSiteId(source.getSiteId());
                    entity.setLastUpdatedAt(Timestamp.valueOf(source.getTimestamp()));

                    // Persistence of accumulators (Critical for Billing Microservice)
                    entity.setTotalGridImportKwh(source.getTotalGridImportKwh());
                    entity.setTotalGridExportKwh(source.getTotalGridExportKwh());
                    entity.setTotalSolarYieldKwh(source.getTotalSolarYieldKwh());
                    entity.setTotalHomeUsageKwh(source.getTotalHomeUsageKwh());

                    // State
                    entity.setChargingStatus(source.getChargingStatus());
                    entity.setBatteryCapacityWh(source.getBatteryCapacityWh());
                    entity.setBatteryRemainingWh(source.getBatteryRemainingWh());
                    entity.setBatterySoc(source.getBatterySoc()); // Calculated helper in POJO

                    return entity;
                });

        return mapper;
    }
}
