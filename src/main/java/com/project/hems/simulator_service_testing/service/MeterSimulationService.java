package com.project.hems.simulator_service_testing.service;

import com.project.hems.simulator_service_testing.domain.MeterEntity;
import com.project.hems.simulator_service_testing.model.ChargingStatus;
import com.project.hems.simulator_service_testing.model.MeterSnapshot;
import com.project.hems.simulator_service_testing.repository.MeterRepository;
import com.project.hems.simulator_service_testing.web.exception.InvalidBatteryStatusException;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.modelmapper.ModelMapper;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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

    private String rawEnergyTopic;

    private static final double DELTA_SECONDS = 5.0;
    private static final double SECONDS_TO_HOURS = 1.0 / 3600.0;

    private void simulateVoltage(MeterSnapshot meter) {
        double noise = (Math.random() * 10) - 5;
        meter.setCurrentVoltage(230.0 + noise);
    }

    private void simulatePowerFluctuation(MeterSnapshot meter, double maxPower) {
        if (Math.random() < 0.1) {
            meter.setCurrentPower(Math.random() * maxPower);
        }
    }

    // @Scheduled(fixedRate = 300000)
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

    // @Scheduled(fixedRate = 5000)
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

            switch (meter.getChargingStatus()) {
                case CHARGING -> simulateBatteryCharging(meter);
                case DISCHARGING -> simulateBatteryDischarging(meter);
                case CHARGED -> {
                    log.trace("Meter [{}]: already charged", userId);
                    continue;
                }
                default -> throw new InvalidBatteryStatusException(
                        "Invalid battery status for meter " + meter.getMeterId());
            }

            log.debug("simulateLiveReadings: sending live data to kafka with topic = " + rawEnergyTopic);
            log.debug("simulateLiveReadings: sending live data to kafka with value = " + meter);
            kafkaTemplate.send(rawEnergyTopic, meter);

            redisTemplate.opsForValue()
                    .set(userId, meter, 10, TimeUnit.SECONDS);
        }
    }

    private void simulateBatteryCharging(MeterSnapshot meter) {

        simulateVoltage(meter);
        simulatePowerFluctuation(meter, 2000); // W

        double powerW = meter.getCurrentPower();

        // Energy added this tick
        double energyAddedWh = powerW * DELTA_SECONDS * SECONDS_TO_HOURS;

        double updatedWh = meter.getBatteryRemainingWh() + energyAddedWh;

        if (updatedWh >= meter.getBatteryCapacityWh()) {
            meter.setBatteryRemainingWh(meter.getBatteryCapacityWh());
            meter.setChargingStatus(ChargingStatus.CHARGED);
            meter.setCurrentPower(0.0);
        } else {
            meter.setBatteryRemainingWh(updatedWh);
        }

        // Grid-side energy (optional, but consistent)
        meter.setTotalEnergyKwh(
                meter.getTotalEnergyKwh() + (energyAddedWh / 1000.0));
    }

    private void simulateBatteryDischarging(MeterSnapshot meter) {

        simulateVoltage(meter);
        simulatePowerFluctuation(meter, 2000); // W

        double powerW = meter.getCurrentPower();

        double energyUsedWh = powerW * DELTA_SECONDS * SECONDS_TO_HOURS;

        double updatedWh = meter.getBatteryRemainingWh() - energyUsedWh;

        if (updatedWh <= 0) {
            meter.setBatteryRemainingWh(0.0);
            meter.setChargingStatus(ChargingStatus.CHARGING);
        } else {
            meter.setBatteryRemainingWh(updatedWh);
        }

        meter.setTotalEnergyKwh(
                meter.getTotalEnergyKwh() + (energyUsedWh / 1000.0));
    }

}
