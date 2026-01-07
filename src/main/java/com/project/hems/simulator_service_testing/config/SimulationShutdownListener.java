package com.project.hems.simulator_service_testing.config;

import java.util.Collection;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.project.hems.simulator_service_testing.domain.MeterEntity;
import com.project.hems.simulator_service_testing.model.MeterSnapshot;
import com.project.hems.simulator_service_testing.repository.MeterRepository;
import com.project.hems.simulator_service_testing.service.MeterSimulationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SimulationShutdownListener {

    private final MeterRepository meterRepository;
    private final MeterSimulationService meterSimulationService;

    @EventListener(ContextClosedEvent.class)
    public void onShutdown() {
        log.info("Saving simulation state from Redis to DB...");

        Collection<MeterSnapshot> allMeters = meterSimulationService.getAllMeters();

        for (MeterSnapshot meter : allMeters) {
            meterRepository.findByUserId(meter.getUserId())
                    .ifPresentOrElse(entity -> {
                        entity.setLastKnownKwh(meter.getTotalEnergyKwh());
                        meterRepository.save(entity);
                    }, () -> {
                        MeterEntity meterEntity = new MeterEntity();
                        meterEntity.setUserId(meter.getUserId());
                        meterEntity.setLastKnownKwh(meter.getTotalEnergyKwh());
                        meterRepository.save(meterEntity);
                    });
        }

        log.info("Shutdown persistence completed successfully");
    }
}
