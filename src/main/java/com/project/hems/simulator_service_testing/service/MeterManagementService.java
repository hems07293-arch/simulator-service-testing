package com.project.hems.simulator_service_testing.service;

import com.project.hems.simulator_service_testing.config.SimulationRedisProperties;
import com.project.hems.simulator_service_testing.domain.MeterEntity;
import com.project.hems.simulator_service_testing.model.BatteryMode;
import com.project.hems.simulator_service_testing.model.ChargingStatus;
import com.project.hems.simulator_service_testing.model.MeterSnapshot;
import com.project.hems.simulator_service_testing.repository.MeterRepository;

import jakarta.annotation.Nullable;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Setter
@Service
public class MeterManagementService {
    // Inject the Redis Template configured earlier
    private final RedisTemplate<String, MeterSnapshot> redisTemplate;
    private final MeterRepository meterRepository;
    private final ModelMapper mapper;
    private final SimulationRedisProperties simulationRedisProperties;

    // 1. Create / Activate a meter (Persist to DB + Cache to Redis)
    @Transactional
    public void activateMeter(Long userId, Double batteryCapacity) {

        // Entry log — helps trace meter lifecycle events
        log.info("activateMeter: activating meter for userId={}", userId);

        // Create an initial snapshot with default electrical values
        MeterSnapshot snapshot = MeterSnapshot.builder()
                .siteId(userId)
                .timestamp(LocalDateTime.now())
                // Physical Hardware Limits
                .batteryCapacityWh(batteryCapacity) // 10 kWh
                .batteryRemainingWh(5000.0) // Start half-full

                // Initial Logic States
                .batteryMode(BatteryMode.AUTO)
                .chargingStatus(ChargingStatus.IDLE)

                // Real-time Power (Start at 0, let simulation take over)
                .solarProductionW(0.0)
                .homeConsumptionW(0.0)
                .batteryPowerW(0.0)
                .gridPowerW(0.0)

                // Cumulative Counters (Start at 0)
                .totalSolarYieldKwh(0.0)
                .totalGridImportKwh(0.0)
                .totalGridExportKwh(0.0)
                .totalHomeUsageKwh(0.0)

                .currentVoltage(230.0)
                .currentAmps(0.0)
                .build();

        log.debug("activateMeter: initial meter snapshot created for userId={}", userId);

        // Persist the meter entity in the database
        MeterEntity saveNewEntityToDb = saveNewEntityToDb(snapshot);

        // Link generated DB meterId back to the snapshot
        snapshot.setMeterId(saveNewEntityToDb.getId());

        log.debug("activateMeter: meter persisted to DB with meterId={}", saveNewEntityToDb.getId());

        // Cache the snapshot in Redis with TTL for fast access
        redisTemplate.opsForValue()
                .set(simulationRedisProperties.getREDIS_KEY() + userId, snapshot, 10, TimeUnit.SECONDS);

        log.info("activateMeter: meter snapshot cached in Redis for userId={} with TTL=10s", userId);
    }

    private MeterEntity saveNewEntityToDb(MeterSnapshot snapshot) {

        // Async persistence — does not block calling thread
        log.debug("saveNewEntityToDb: saving meter entity for userId={}", snapshot.getSiteId());

        MeterEntity savedEntity = meterRepository.save(mapper.map(snapshot, MeterEntity.class));

        log.debug("saveNewEntityToDb: meter entity saved successfully [meterId={}, userId={}]",
                savedEntity.getId(), savedEntity.getSiteId());

        return savedEntity;
    }

    // 3. Get Data (Read from Redis by userId)
    public MeterSnapshot getMeterData(Long userId) {

        log.debug("getMeterData: fetching meter snapshot from Redis for userId={}", userId);

        MeterSnapshot snapshot = redisTemplate.opsForValue()
                .get(simulationRedisProperties.getREDIS_KEY() + userId.toString());

        if (snapshot == null) {
            log.warn("getMeterData: no meter snapshot found in Redis for userId={}", userId);
        } else {
            log.debug("getMeterData: meter snapshot retrieved successfully for userId={}", userId);
        }

        return snapshot;
    }

    // Fetch all meter snapshots as a list
    public List<MeterSnapshot> getAllMeters() {

        log.debug("getAllMeters: fetching all meter keys from Redis");

        Set<String> keys = redisTemplate.keys(simulationRedisProperties.getREDIS_KEY() + "*");

        if (keys == null || keys.isEmpty()) {
            log.warn("getAllMeters: no meter keys found in Redis");
            return List.of();
        }

        log.debug("getAllMeters: {} meter keys found, fetching values", keys.size());

        List<MeterSnapshot> snapshots = redisTemplate.opsForValue()
                .multiGet(keys);

        log.info("getAllMeters: fetched {} meter snapshots from Redis",
                snapshots != null ? snapshots.size() : 0);

        return snapshots;
    }

    // 4. Get All Data as Map<Key, Snapshot>
    public Map<String, MeterSnapshot> getAllMeterSnapshot() {

        log.debug("getAllMeterSnapshot: fetching all meter key-value pairs from Redis");

        Set<String> keys = redisTemplate.keys(simulationRedisProperties.getREDIS_KEY() + "*");

        if (keys == null || keys.isEmpty()) {
            log.warn("getAllMeterSnapshot: no Redis keys found with pattern={}",
                    simulationRedisProperties.getREDIS_KEY());
            return Collections.emptyMap();
        }

        log.debug("getAllMeterSnapshot: {} meter keys found", keys.size());

        Map<String, MeterSnapshot> meterReadings = new HashMap<>();

        for (String keyVal : keys) {

            log.trace("getAllMeterSnapshot: fetching value for key={}", keyVal);

            @Nullable
            MeterSnapshot meterSnapshot = redisTemplate.opsForValue()
                    .get(keyVal);

            if (meterSnapshot == null) {
                log.warn("getAllMeterSnapshot: null snapshot found for key={}", keyVal);
            } else {
                log.trace("getAllMeterSnapshot: snapshot retrieved for key={}", keyVal);
            }

            meterReadings.put(keyVal, meterSnapshot);
        }

        log.info("getAllMeterSnapshot: successfully assembled snapshot map with {} entries",
                meterReadings.size());

        return meterReadings;
    }

    // Load all meter data from DB into Redis (cache warm-up / recovery path)
    public void getValuesFromDB() {

        log.warn("getValuesFromDB: Redis cache miss detected, loading meter data from database");

        List<MeterEntity> allMeterReading = meterRepository.findAll();

        log.debug("getValuesFromDB: {} meter records fetched from database", allMeterReading.size());

        allMeterReading.forEach(meterEntity -> {

            // Convert DB entity → snapshot before caching
            MeterSnapshot snapshot = mapper.map(meterEntity, MeterSnapshot.class);

            redisTemplate.opsForValue()
                    .set(
                            simulationRedisProperties.getREDIS_KEY() + meterEntity.getSiteId().toString(),
                            snapshot,
                            10,
                            TimeUnit.SECONDS);

            log.trace("getValuesFromDB: cached meter snapshot for userId={} with TTL=10s",
                    meterEntity.getSiteId());
        });

        log.info("getValuesFromDB: Redis cache successfully repopulated from database");
    }

}
