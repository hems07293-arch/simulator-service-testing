package com.project.hems.simulator_service_testing.config;

import java.util.Collection;

import com.project.hems.simulator_service_testing.service.MeterManagementService;

import org.modelmapper.ModelMapper;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.project.hems.simulator_service_testing.domain.MeterEntity;
import com.project.hems.simulator_service_testing.model.MeterSnapshot;
import com.project.hems.simulator_service_testing.repository.MeterRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SimulationShutdownListener {

    private final MeterRepository meterRepository;
    private final ModelMapper mapper;
    private final MeterManagementService meterManagementService;

    @EventListener(ContextClosedEvent.class)
    public void onShutdown() {
        log.warn("onShutdown: Application context closing. Syncing high-fidelity state to Cold Storage...");

        // 1. Get all "Hot" data from Redis
        Collection<MeterSnapshot> allSnapshots = meterManagementService.getAllMeters();

        if (allSnapshots.isEmpty()) {
            log.info("onShutdown: No active snapshots in Redis to persist.");
            return;
        }

        log.info("onShutdown: Syncing {} meters to PostgreSQL", allSnapshots.size());

        for (MeterSnapshot snapshot : allSnapshots) {
            try {
                meterRepository.findBySiteId(snapshot.getSiteId())
                        .ifPresentOrElse(entity -> {
                            // Update existing record using the new mapping logic
                            updateEntityFromSnapshot(entity, snapshot);
                            meterRepository.save(entity);
                            log.debug("onShutdown: Updated Meter ID {}", entity.getId());
                        }, () -> {
                            // Fallback: Create new record if user doesn't exist in DB
                            MeterEntity newEntity = mapper.map(snapshot, MeterEntity.class);
                            meterRepository.save(newEntity);
                            log.debug("onShutdown: Created new record for User ID {}", snapshot.getSiteId());
                        });
            } catch (Exception e) {
                log.error("onShutdown: Failed to persist meter for user {}: {}", snapshot.getSiteId(), e.getMessage());
            }
        }

        log.info("onShutdown: Persistence completed successfully.");
    }

    /**
     * Helper to update only the fields that change during simulation.
     * This ensures we don't accidentally overwrite static metadata.
     */
    private void updateEntityFromSnapshot(MeterEntity entity, MeterSnapshot snapshot) {
        // Accumulators (The "Odometer" readings)
        entity.setTotalSolarYieldKwh(snapshot.getTotalSolarYieldKwh());
        entity.setTotalGridImportKwh(snapshot.getTotalGridImportKwh());
        entity.setTotalGridExportKwh(snapshot.getTotalGridExportKwh());
        entity.setTotalHomeUsageKwh(snapshot.getTotalHomeUsageKwh());

        // Battery State
        entity.setBatteryRemainingWh(snapshot.getBatteryRemainingWh());
        entity.setBatterySoc(snapshot.getBatterySoc()); // Uses the helper in your POJO
        entity.setChargingStatus(snapshot.getChargingStatus());
    }

}
