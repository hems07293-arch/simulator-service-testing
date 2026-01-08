package com.project.hems.simulator_service_testing.service;

import com.project.hems.simulator_service_testing.domain.MeterEntity;
import com.project.hems.simulator_service_testing.model.MeterSnapshot;
import com.project.hems.simulator_service_testing.repository.MeterRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.jspecify.annotations.Nullable;
import org.modelmapper.ModelMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeterSimulationService {

    // Inject the Redis Template configured earlier
    private final RedisTemplate<String, MeterSnapshot> redisTemplate;
    private final MeterRepository meterRepository;
    private final ModelMapper mapper;

    // The Key used in Redis to store our Map
    private static final String REDIS_KEY = "simulator:userId:";

    // 1. Create/Activate a meter (Save to Redis Hash)
    public void activateMeter(Long userId) {
        log.debug("Activating meter for userId: {}", userId);

        MeterSnapshot snapshot = MeterSnapshot.builder()
                .userId(userId)
                .totalEnergyKwh(0.0)
                .currentVoltage(230.0)
                .currentPower(0.0)
                .build();

        MeterEntity saveNewEntityToDb = saveNewEntityToDb(snapshot);
        snapshot.setMeterId(saveNewEntityToDb.getId());

        redisTemplate.opsForValue()
                .set(REDIS_KEY + userId, snapshot, 10, TimeUnit.SECONDS);
        // redisTemplate.opsForHash().put(REDIS_KEY, userId.toString(), snapshot);
    }

    @Async
    private MeterEntity saveNewEntityToDb(MeterSnapshot snapshot) {
        MeterEntity meterEntity = new MeterEntity();
        meterEntity.setUserId(snapshot.getUserId());
        meterEntity.setLastKnownKwh(snapshot.getTotalEnergyKwh());

        return meterRepository.save(meterEntity);
    }

    // 2. The "Heartbeat": Updates every 1 second
    @Scheduled(fixedRate = 5000)
    public void simulateLiveReadings() {
        log.debug("simulateLiveReadings: starting live simulation power output for ach meter");
        // Fetch ALL meters from Redis in one go (Efficiency!)

        log.debug("fetching all entries of meter from redis");
        Map<String, MeterSnapshot> allMeterSnapshot = getAllMeterSnapshot();
        // Map<Object, Object> rawMap = redisTemplate.opsForHash().entries(REDIS_KEY);

        if (allMeterSnapshot.isEmpty()) {
            log.warn("no data found from redis fetching the data from db");
            getValuesFromDB();
        }

        log.debug("simulateLiveReadings: Simulating live readings for {} meters", allMeterSnapshot.size());

        for (Map.Entry<String, MeterSnapshot> entry : allMeterSnapshot.entrySet()) {
            String userIdStr = entry.getKey();
            MeterSnapshot meter = entry.getValue();

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
            redisTemplate.opsForValue()
                    .set(userIdStr, meter, 10, TimeUnit.SECONDS);
            // redisTemplate.opsForHash().put(REDIS_KEY, userIdStr, meter);
        }
    }

    private void getValuesFromDB() {
        List<MeterEntity> allMeterReading = meterRepository.findAll();
        log.debug("simulateLiveReadings: fecthing all meter reading from db and putting in redis...");

        allMeterReading.forEach(meterEntity -> {
            redisTemplate.opsForValue()
                    .set(REDIS_KEY + meterEntity.getUserId().toString(), mapper.map(meterEntity, MeterSnapshot.class),
                            10,
                            TimeUnit.SECONDS);
        });
        log.debug("simulateLiveReadings: successfully fetched all meter readings");
    }

    // 3. Get Data (Read from Redis)
    public MeterSnapshot getMeterData(Long userId) {
        // Fetch specific key from Hash
        return redisTemplate.opsForValue()
                .get(REDIS_KEY + userId.toString());
    }

    // 4. Get All Data
    public Map<String, MeterSnapshot> getAllMeterSnapshot() {
        log.debug("fetching all key value pair for meter reading");
        Set<String> keys = redisTemplate.keys(REDIS_KEY + "*");

        if (keys == null || keys.isEmpty()) {
            log.debug("unable to find the key set with this pattern = " + REDIS_KEY);
            return Collections.emptyMap();
        }

        Map<String, MeterSnapshot> meterReadings = new HashMap<>();

        for (String keyVal : keys) {
            log.debug("making a map with key = " + keyVal);

            @Nullable
            MeterSnapshot meterSnapshot = redisTemplate.opsForValue()
                    .get(keyVal);

            log.debug("making a map with value = " + meterSnapshot);
            meterReadings.put(keyVal, meterSnapshot);
        }

        return meterReadings;
    }

    public List<MeterSnapshot> getAllMeters() {
        // Fetch all values from Hash
        Set<String> keys = redisTemplate.keys(REDIS_KEY + "*");
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }

        return redisTemplate.opsForValue()
                .multiGet(keys);
    }
}