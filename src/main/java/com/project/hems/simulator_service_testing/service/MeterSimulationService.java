package com.project.hems.simulator_service_testing.service;

import com.project.hems.simulator_service_testing.model.MeterSnapshot;
import com.project.hems.simulator_service_testing.repository.MeterRepository;
import com.project.hems.simulator_service_testing.web.exception.MeterStatusAlreadyPresentException;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeterSimulationService {

    // We only keep a list of "Who is active". The "Data" is in Redis.
    private final Set<Long> activeUserIds = ConcurrentHashMap.newKeySet();

    private final MeterRedisService redisService; // Your upgraded publisher
    private final MeterRepository meterRepository;

    // 1. Activate: Add ID to our list & Initialize Redis State
    public void activateMeter(Long userId) {
        log.debug("Activating meter for userId: " + userId);

        if (activeUserIds.contains(userId)) {
            throw new MeterStatusAlreadyPresentException(
                    "invalid user id, meter status already present with given user id = " + userId);
        }
        // Add to our "To-Do List" for simulation
        activeUserIds.add(userId);

        // Create initial state and push to Redis immediately
        MeterSnapshot initialSnapshot = MeterSnapshot.builder()
                .userId(userId)
                .totalEnergyKwh(0.0)
                .currentVoltage(230.0)
                .currentPower(0.0)
                .build();

        redisService.publish(initialSnapshot);
    }

    // 2. The Loop: Fetch from Redis -> Update -> Save to Redis
    @Scheduled(fixedRate = 1000)
    public void simulateLiveReadings() {
        if (activeUserIds.isEmpty())
            return;

        for (Long userId : activeUserIds) {
            // A. FETCH from Redis (The "Read" step)
            MeterSnapshot snapshot = redisService.getSnapshot(userId);

            // Safety check: If Redis key expired (TTL), recreate or skip
            if (snapshot == null) {
                // Option: Reload from DB or skip. Let's skip for now.
                log.warn("Meter data missing in Redis for user " + userId);
                continue;
            }

            // B. MODIFY (The Simulation Logic)
            // Voltage
            double noise = (Math.random() * 10) - 5;
            snapshot.setCurrentVoltage(230.0 + noise);

            // Power
            if (Math.random() < 0.1) {
                snapshot.setCurrentPower(Math.random() * 2000);
            }

            // Energy (Accumulation)
            double kwhIncrement = (snapshot.getCurrentPower() * 1) / 3_600_000.0;
            snapshot.setTotalEnergyKwh(snapshot.getTotalEnergyKwh() + kwhIncrement);

            // C. WRITE (The "Publish" step)
            redisService.publish(snapshot);
        }
    }

    // 3. Database Persistence (Checkpoint)
    @PreDestroy
    public void saveDataToDb() {
        log.info("Saving Redis state to Database on Shutdown...");
        for (Long userId : activeUserIds) {
            // Get final state from Redis
            MeterSnapshot snapshot = redisService.getSnapshot(userId);

            if (snapshot != null) {
                // Save only the kWh to SQL DB
                meterRepository.findByUserId(userId).ifPresentOrElse(entity -> {
                    entity.setLastKnownKwh(snapshot.getTotalEnergyKwh());
                    meterRepository.save(entity);
                }, () -> {
                    // Create new if needed...
                });
            }
        }
    }
}