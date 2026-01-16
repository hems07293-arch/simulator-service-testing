package com.project.hems.simulator_service_testing.service;

import com.project.hems.simulator_service_testing.domain.MeterEntity;
import com.project.hems.simulator_service_testing.model.MeterSnapshot;
import com.project.hems.simulator_service_testing.repository.MeterRepository;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.modelmapper.ModelMapper;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Setter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "property.config.kafka")
public class MeterSimulationService {

    // Inject the Redis Template configured earlier
    private final RedisTemplate<String, MeterSnapshot> redisTemplate;
    private final MeterManagementService meterManagementService;
    private final MeterRepository meterRepository;
    private final ModelMapper mapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final EnergyPhysicsEngine energyPhysicsEngine;
    private final EnvironmentSimulator environmentSimulator;

    private String rawEnergyTopic;

    @Scheduled(fixedRate = 300000)
    public void saveMeterSnapshotToDB() {
        log.debug("saveMeterSnapshotToDB: scheduler triggered");

        Map<String, MeterSnapshot> snapshots = meterManagementService.getAllMeterSnapshot();

        if (snapshots.isEmpty()) {
            log.warn("saveMeterSnapshotToDB: Redis empty, returning back");
            return;
        }

        log.info("saveMeterSnapshotToDB: storing {} meters", snapshots.size());

        for (Map.Entry<String, MeterSnapshot> entry : snapshots.entrySet()) {

            MeterSnapshot meter = entry.getValue();

            meterRepository.save(mapper.map(meter, MeterEntity.class));
        }
    }

    @Scheduled(fixedRate = 5000)
    public void simulateLiveReadings() {

        log.debug("simulateLiveReadings: scheduler triggered");

        Map<String, MeterSnapshot> snapshots = meterManagementService.getAllMeterSnapshot();

        if (snapshots.isEmpty()) {
            log.warn("simulateLiveReadings: Redis empty, loading from DB");
            meterManagementService.getValuesFromDB();
            return;
        }

        log.info("simulateLiveReadings: simulating {} meters", snapshots.size());

        for (Map.Entry<String, MeterSnapshot> entry : snapshots.entrySet()) {

            String userId = entry.getKey();
            MeterSnapshot meter = entry.getValue();

            // 1. Environmental Inputs
            double solarW = environmentSimulator.calculateSolarProduction();
            double loadW = environmentSimulator.calculateHomeConsumption();

            // 2. Physics Engine (Priority Logic)
            energyPhysicsEngine.processEnergyBalance(meter, solarW, loadW);

            // 3. Electrical Noise (Voltage/Amps for realism)
            environmentSimulator.applyElectricalMetadata(meter);

            log.debug("simulateLiveReadings: sending live data to kafka with topic = " + rawEnergyTopic);
            log.debug("simulateLiveReadings: sending live data to kafka with value = " + meter);
            meter.setTimestamp(LocalDateTime.now());
            kafkaTemplate.send(rawEnergyTopic, meter);

            redisTemplate.opsForValue()
                    .set(userId, meter, 10, TimeUnit.SECONDS);
        }
    }

}
