package com.project.hems.simulator_service_testing.config;

import java.util.Collection;

import com.project.hems.simulator_service_testing.service.MeterManagementService;
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
    private final MeterManagementService meterManagementService;

    @EventListener(ContextClosedEvent.class)
    public void onShutdown() {

        // Application shutdown hook — last chance to persist in-memory / cached state
        log.warn("onShutdown: application context closing, persisting meter state from Redis to DB");

        // Fetch all currently cached meter snapshots
        Collection<MeterSnapshot> allMeters = meterManagementService.getAllMeters();

        log.info("onShutdown: {} meter snapshots retrieved from Redis for persistence",
                allMeters.size());

        // Iterate through each snapshot and sync state to the database
        for (MeterSnapshot meter : allMeters) {

            log.debug("onShutdown: persisting meter state for userId={}", meter.getUserId());

            meterRepository.findByUserId(meter.getUserId())
                    .ifPresentOrElse(entity -> {

                        // Existing meter found — update last known energy value
                        entity.setLastKnownKwh(meter.getTotalEnergyKwh());
                        meterRepository.save(entity);

                        log.trace("onShutdown: updated existing meter [userId={}, lastKnownKwh={}]",
                                meter.getUserId(), meter.getTotalEnergyKwh());

                    }, () -> {

                        // No existing meter — create a new DB record as a safety fallback
                        MeterEntity meterEntity = new MeterEntity();
                        meterEntity.setUserId(meter.getUserId());
                        meterEntity.setLastKnownKwh(meter.getTotalEnergyKwh());
                        meterRepository.save(meterEntity);

                        log.trace("onShutdown: created new meter entity [userId={}, lastKnownKwh={}]",
                                meter.getUserId(), meter.getTotalEnergyKwh());
                    });
        }

        // Final confirmation — critical for graceful shutdown diagnostics
        log.info("onShutdown: meter state persistence completed successfully");
    }

}
