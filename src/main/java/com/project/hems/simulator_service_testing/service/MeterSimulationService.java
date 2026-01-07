package com.project.hems.simulator_service_testing.service;

import com.project.hems.simulator_service_testing.domain.MeterEntity;
import com.project.hems.simulator_service_testing.model.VirtualSmartMeter;
import com.project.hems.simulator_service_testing.repository.MeterRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeterSimulationService {

    private final Map<Long, VirtualSmartMeter> activeMeters;
    private final MeterRepository meterRepository;

    // 1. Create a meter when a user logs in / registers
    public void activateMeter(Long userId, double lastSavedKwh) {
        log.debug("Activating meter for userId: " + userId);
        activeMeters.put(userId, new VirtualSmartMeter(userId, lastSavedKwh));
    }

    // 2. The "Heartbeat": Updates every 1 second
    @Scheduled(fixedRate = 1000)
    public void simulateLiveReadings() {
        log.debug("Simulating live readings for userId: " + activeMeters.keySet());
        for (VirtualSmartMeter meter : activeMeters.values()) {
            // Simulate Voltage Fluctuation (225V - 235V)
            double noise = (Math.random() * 10) - 5;
            meter.setCurrentVoltage(230.0 + noise);
            log.info("Current voltage: " + meter.getCurrentVoltage());

            // Simulate Power Usage (Randomly turning appliances on/off)
            // Logic: 10% chance to change load drastically, otherwise stable
            if (Math.random() < 0.1) {
                log.info("Noise is set to: " + noise);
                meter.setCurrentPower(Math.random() * 2000); // 0 to 2000 Watts
            }
            log.info("Current power: " + meter.getCurrentPower());

            // ACCUMULATE ENERGY (Physics: Power * Time)
            // 1000W for 1 hour = 1 kWh.
            // Since this runs every 1 second: kWh += (Watts * 1s) / (1000 * 3600)
            double kwhIncrement = (meter.getCurrentPower() * 1) / 3_600_000.0;
            log.info("Current kwh increment: " + kwhIncrement);
            meter.setTotalEnergyKwh(meter.getTotalEnergyKwh() + kwhIncrement);
            log.info("Total energy kwh: " + meter.getTotalEnergyKwh());
        }
    }

    // 3. Method for the Controller to get data
    public VirtualSmartMeter getMeterData(Long userId) {
        log.debug("Retrieving meter data for userId: " + userId);
        return activeMeters.get(userId);
    }

    // 4. Method for the Database Saver to get all data
    public Collection<VirtualSmartMeter> getAllMeters() {
        log.debug("Retrieving meter data");
        return activeMeters.values();
    }

    @PreDestroy
    public void saveDataToDb() {
        log.info("saving the data to db before closing application");
        for (VirtualSmartMeter meter : activeMeters.values()) {
            log.debug("iterating all meter from all sites and saving them");

            // Find Entity by User ID
            Optional<MeterEntity> optionalMeterEntity = meterRepository.findByUserId(meter.getUserId());

            optionalMeterEntity.ifPresentOrElse(entity -> {
                log.debug("already present meter then just update the total energy in kwh");
                entity.setLastKnownKwh(meter.getTotalEnergyKwh());
                meterRepository.save(entity);
            }, () -> {
                log.debug("meter not present in db creating a new meter reading in db");
                MeterEntity meterEntity = new MeterEntity();
                meterEntity.setUserId(meter.getUserId());
                meterEntity.setLastKnownKwh(meter.getTotalEnergyKwh());
                meterRepository.save(meterEntity);
            });
        }
    }
}
