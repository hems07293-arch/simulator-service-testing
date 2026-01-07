package com.project.hems.simulator_service_testing.service;

import com.project.hems.simulator_service_testing.domain.MeterEntity;
import com.project.hems.simulator_service_testing.model.MeterSnapshot;
import com.project.hems.simulator_service_testing.repository.MeterRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeterSimulationService {

    // Inject the Redis Template configured earlier
    private final RedisTemplate<String, MeterSnapshot> redisTemplate;
    private final MeterRepository meterRepository;

    // The Key used in Redis to store our Map
    private static final String REDIS_KEY = "ACTIVE_SIMULATIONS";

    // 1. Create/Activate a meter (Save to Redis Hash)
    public void activateMeter(Long userId) {
        log.debug("Activating meter for userId: {}", userId);

        MeterSnapshot snapshot = MeterSnapshot.builder()
                .userId(userId)
                .totalEnergyKwh(0.0)
                .currentVoltage(230.0)
                .currentPower(0.0)
                .build();

        saveNewEntityToDb(snapshot);

        redisTemplate.opsForHash().put(REDIS_KEY, userId.toString(), snapshot);
    }

    @Async
    private void saveNewEntityToDb(MeterSnapshot snapshot) {
        MeterEntity meterEntity = new MeterEntity();
        meterEntity.setUserId(snapshot.getUserId());
        meterEntity.setLastKnownKwh(snapshot.getTotalEnergyKwh());

        meterRepository.save(meterEntity);
    }

    // 2. The "Heartbeat": Updates every 1 second
    @Scheduled(fixedRate = 1000)
    public void simulateLiveReadings() {
        log.debug("simulateLiveReadings: starting live simulation power output for ach meter");
        // Fetch ALL meters from Redis in one go (Efficiency!)
        Map<Object, Object> rawMap = redisTemplate.opsForHash().entries(REDIS_KEY);
        log.debug("fetching all entries of meter from redis");
        if (rawMap.isEmpty()) {
            log.warn("no data found from redis fetching the data from db");
            getValuesFromDB();
        }

        log.debug("simulateLiveReadings: Simulating live readings for {} meters", rawMap.size());

        for (Map.Entry<Object, Object> entry : rawMap.entrySet()) {
            String userIdStr = (String) entry.getKey();
            MeterSnapshot meter = (MeterSnapshot) entry.getValue();

            // --- SIMULATION LOGIC ---

            // 1. Voltage Noise
            double noise = (Math.random() * 10) - 5;
            meter.setCurrentVoltage(230.0 + noise);

            // 2. Power Fluctuation
            if (Math.random() < 0.1) {
                meter.setCurrentPower(Math.random() * 2000);
            }

            // 3. Accumulate Energy
            double kwhIncrement = (meter.getCurrentPower() * 1) / 3_600_000.0;
            meter.setTotalEnergyKwh(meter.getTotalEnergyKwh() + kwhIncrement);

            // --- WRITE BACK TO REDIS ---
            // Unlike Java Map, we MUST explicitly save the updated object back to Redis
            redisTemplate.opsForHash().put(REDIS_KEY, userIdStr, meter);
        }
    }

    private void getValuesFromDB() {
        List<MeterEntity> allMeterReading = meterRepository.findAll();
        log.debug("simulateLiveReadings: fecthing all meter reading from db and putting in redis...");

        allMeterReading.forEach(meterEntity -> {
            redisTemplate.opsForHash().put(REDIS_KEY, meterEntity, allMeterReading);
        });
    }

    // 3. Get Data (Read from Redis)
    public MeterSnapshot getMeterData(Long userId) {
        // Fetch specific key from Hash
        return (MeterSnapshot) redisTemplate.opsForHash().get(REDIS_KEY, userId.toString());
    }

    // 4. Get All Data
    public Collection<MeterSnapshot> getAllMeters() {
        // Fetch all values from Hash
        return redisTemplate.opsForHash().values(REDIS_KEY).stream()
                .map(obj -> (MeterSnapshot) obj)
                .toList();
    }
}