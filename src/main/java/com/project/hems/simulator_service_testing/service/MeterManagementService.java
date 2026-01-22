package com.project.hems.simulator_service_testing.service;

import com.project.hems.simulator_service_testing.domain.MeterEntity;
import com.project.hems.simulator_service_testing.model.BatteryMode;
import com.project.hems.simulator_service_testing.model.ChargingStatus;
import com.project.hems.simulator_service_testing.model.MeterSnapshot;
import com.project.hems.simulator_service_testing.repository.MeterRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Setter
@Service
public class MeterManagementService {
    private final Map<String, MeterSnapshot> meterReadings;
    private final MeterRepository meterRepository;
    private final ModelMapper mapper;

    // 1. Create / Activate a meter (Persist to DB + Cache to Bean Map)
    @Transactional
    public void activateMeter(Long siteId, Double batteryCapacity) {

        // Entry log — helps trace meter lifecycle events
        log.info("activateMeter: activating meter for siteId={}", siteId);

        // Create an initial snapshot with default electrical values
        MeterSnapshot snapshot = MeterSnapshot.builder()
                .siteId(siteId)
                .timestamp(LocalDateTime.now())
                // Physical Hardware Limits
                .batteryCapacityWh(batteryCapacity) // 10 kWh
                .batteryRemainingWh(5000.0) // Start half-full

                // Initial Logic States
                .batterySoc(50)
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

        log.debug("activateMeter: initial meter snapshot created for siteId={}", siteId);

        // Persist the meter entity in the database
        MeterEntity saveNewEntityToDb = saveNewEntityToDb(snapshot);

        // Link generated DB meterId back to the snapshot
        snapshot.setMeterId(saveNewEntityToDb.getId());

        log.debug("activateMeter: meter persisted to DB with meterId={}", saveNewEntityToDb.getId());

        // Cache the snapshot in bean map for fast access
        meterReadings.put(siteId.toString(), snapshot);

        log.info("activateMeter: meter snapshot cached in Bean Map for siteId={} with TTL=10s", siteId);
    }

    private MeterEntity saveNewEntityToDb(MeterSnapshot snapshot) {

        // Async persistence — does not block calling thread
        log.debug("saveNewEntityToDb: saving meter entity for siteId={}", snapshot.getSiteId());

        MeterEntity savedEntity = meterRepository.save(mapper.map(snapshot, MeterEntity.class));

        log.debug("saveNewEntityToDb: meter entity saved successfully [meterId={}, siteId={}]",
                savedEntity.getId(), savedEntity.getSiteId());

        return savedEntity;
    }

    // 3. Get Data (Read from map by siteId)
    public MeterSnapshot getMeterData(Long siteId) {

        log.debug("getMeterData: fetching meter snapshot from Bean Map for siteId={}", siteId);

        MeterSnapshot snapshot = meterReadings.get(siteId.toString());

        if (snapshot == null) {
            log.warn("getMeterData: no meter snapshot found in Bean Map for siteId={}", siteId);
        } else {
            log.debug("getMeterData: meter snapshot retrieved successfully for siteId={}", siteId);
        }

        return snapshot;
    }

    // Fetch all meter snapshots as a list
    public List<MeterSnapshot> getAllMeters() {

        log.debug("getAllMeters: fetching all meter keys from Bean Map");

        List<MeterSnapshot> snapshots = meterReadings.entrySet().stream()
                .map(entry -> entry.getValue())
                .collect(Collectors.toList());

        log.info("getAllMeters: fetched {} meter snapshots from Bean Map",
                snapshots != null ? snapshots.size() : 0);

        return snapshots;
    }

    // Load all meter data from DB into Bean Map (cache warm-up / recovery path)
    public void getValuesFromDB() {

        List<MeterEntity> allMeterReading = meterRepository.findAll();

        log.debug("getValuesFromDB: {} meter records fetched from database", allMeterReading.size());

        allMeterReading.forEach(meterEntity -> {

            // Convert DB entity → snapshot before caching
            MeterSnapshot snapshot = mapper.map(meterEntity, MeterSnapshot.class);

            meterReadings.put(meterEntity.getSiteId().toString(), snapshot);

            log.trace("getValuesFromDB: cached meter snapshot for siteId={} with TTL=10s",
                    meterEntity.getSiteId());
        });

        log.info("getValuesFromDB: Bean Map cache successfully repopulated from database");
    }

}
