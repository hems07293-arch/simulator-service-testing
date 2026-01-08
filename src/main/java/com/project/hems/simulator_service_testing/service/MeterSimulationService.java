package com.project.hems.simulator_service_testing.service;

import com.project.hems.simulator_service_testing.model.MeterSnapshot;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Setter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "simulation-redis.config")
public class MeterSimulationService {

    // Inject the Redis Template configured earlier
    private final RedisTemplate<String, MeterSnapshot> redisTemplate;
    private final MeterManagementService meterManagementService;

    // The Key used in Redis to store our Map
    @Value("${simulation-redis.config.redis-key}")
    private String REDIS_KEY ;

    @Scheduled(fixedRate = 5000)
    public void simulateLiveReadings() {

        // Scheduler heartbeat — helps confirm the task is alive
        log.debug("simulateLiveReadings: scheduler triggered");

        // Fetch ALL meter snapshots from Redis in one call (fast path)
        log.debug("simulateLiveReadings: fetching all meter snapshots from Redis");
        Map<String, MeterSnapshot> allMeterSnapshot = meterManagementService.getAllMeterSnapshot();

        // Fallback path: Redis cache miss
        if (allMeterSnapshot.isEmpty()) {
            log.warn("simulateLiveReadings: no meter data found in Redis, falling back to database");
            meterManagementService.getValuesFromDB();
            return; // avoid simulating on empty data
        }

        log.info("simulateLiveReadings: simulating live readings for {} meters", allMeterSnapshot.size());

        // Iterate over each meter snapshot and simulate real-time changes
        for (Map.Entry<String, MeterSnapshot> entry : allMeterSnapshot.entrySet()) {

            String userIdStr = entry.getKey();
            MeterSnapshot meter = entry.getValue();

            log.debug("simulateLiveReadings: simulating data for meter [userId={}]", userIdStr);

            // ---------------- SIMULATION LOGIC ----------------

            // 1. Voltage fluctuation (±5V noise around 230V)
            double noise = (Math.random() * 10) - 5;
            double simulatedVoltage = 230.0 + noise;
            meter.setCurrentVoltage(simulatedVoltage);

            log.trace("Meter [{}]: voltage simulated as {} V", userIdStr, simulatedVoltage);

            // 2. Random power fluctuation (10% chance of sudden change)
            if (Math.random() < 0.1) {
                double simulatedPower = Math.random() * 2000; // watts
                meter.setCurrentPower(simulatedPower);

                log.trace("Meter [{}]: power spike detected, new power = {} W",
                        userIdStr, simulatedPower);
            }

            // 3. Energy accumulation (Wh → kWh conversion)
            // Power (W) × time (1 second) → divide by 3,600,000 to get kWh
            double kwhIncrement = (meter.getCurrentPower() * 1) / 3_600_000.0;
            double updatedTotalEnergy = meter.getTotalEnergyKwh() + kwhIncrement;
            meter.setTotalEnergyKwh(updatedTotalEnergy);

            log.trace("Meter [{}]: energy incremented by {} kWh, total = {} kWh",
                    userIdStr, kwhIncrement, updatedTotalEnergy);

            // ---------------- WRITE BACK TO REDIS ----------------

            // Redis does NOT auto-track object mutations like a Java Map
            // Explicitly overwrite the value with a TTL to keep cache fresh
            redisTemplate.opsForValue()
                    .set(userIdStr, meter, 10, TimeUnit.SECONDS);

            log.debug("simulateLiveReadings: updated meter snapshot saved to Redis [userId={}, ttl=10s]",
                    userIdStr);
        }

        log.debug("simulateLiveReadings: simulation cycle completed");
    }

}