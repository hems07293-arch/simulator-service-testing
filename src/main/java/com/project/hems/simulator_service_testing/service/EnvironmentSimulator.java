package com.project.hems.simulator_service_testing.service;

import org.springframework.stereotype.Component;

import com.project.hems.simulator_service_testing.model.MeterSnapshot;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class EnvironmentSimulator {

    public double calculateSolarProduction() {
        log.debug("calculateSolarProduction: start calculating solar production");

        int hour = java.time.LocalTime.now().getHour();
        log.debug("calculateSolarProduction: current hour = {}", hour);

        // Simple window: 6 AM to 6 PM (18:00)
        if (hour < 6 || hour >= 18) {
            log.debug("calculateSolarProduction: night time detected, solar production = 0.0");
            return 0.0;
        }

        double maxPeakW = 5000.0; // 5kW system
        double radians = Math.PI * (hour - 6) / 12.0;
        double solarProduction = maxPeakW * Math.sin(radians);

        log.debug(
                "calculateSolarProduction: calculated solarProductionW = {} using maxPeakW = {}",
                solarProduction,
                maxPeakW);

        return solarProduction;
    }

    public double calculateHomeConsumption() {
        log.debug("calculateHomeConsumption: start calculating home consumption");

        double baseLoad = 400.0;
        log.debug("calculateHomeConsumption: baseLoadW = {}", baseLoad);

        double noise = (Math.random() * 100) - 50;
        log.debug("calculateHomeConsumption: noiseW = {}", noise);

        double spike = 0.0;
        if (Math.random() < 0.10) {
            spike = 2000.0 + (Math.random() * 2000.0);
            log.info("calculateHomeConsumption: high power spike detected, spikeW = {}", spike);
        }

        double totalLoad = baseLoad + noise + spike;
        double finalLoad = Math.max(totalLoad, 100.0);

        log.debug(
                "calculateHomeConsumption: totalLoadW = {}, finalConsumptionW = {}",
                totalLoad,
                finalLoad);

        return finalLoad;
    }

    public void applyElectricalMetadata(MeterSnapshot meter) {
        log.debug("applyElectricalMetadata: applying electrical metadata to meter");

        double voltage = 230.0 + (Math.random() * 4 - 2);
        meter.setCurrentVoltage(voltage);

        log.debug("applyElectricalMetadata: calculated voltage = {}", voltage);

        double totalPowerW = Math.abs(meter.getHomeConsumptionW());
        double currentAmps = totalPowerW / voltage;
        meter.setCurrentAmps(currentAmps);

        log.debug(
                "applyElectricalMetadata: gridPowerW = {}, calculated currentAmps = {}",
                meter.getGridPowerW(),
                currentAmps);
    }
}
