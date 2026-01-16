package com.project.hems.simulator_service_testing.service;

import org.springframework.stereotype.Component;

import com.project.hems.simulator_service_testing.model.ChargingStatus;
import com.project.hems.simulator_service_testing.model.MeterSnapshot;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class EnergyPhysicsEngine {

    private static final double DELTA_SECONDS = 5.0;
    private static final double SECONDS_TO_HOURS = 1.0 / 3600.0;

    public void processEnergyBalance(MeterSnapshot meter, double solarW, double loadW) {
        log.debug(
                "processEnergyBalance: start | solarW = {}, loadW = {}",
                solarW,
                loadW);

        double remainingLoadW = loadW;
        double batteryFlowW = 0.0;
        double gridFlowW = 0.0;

        // Solar always used first
        double solarUsedW = Math.min(solarW, remainingLoadW);
        remainingLoadW -= solarUsedW;

        log.debug(
                "processEnergyBalance: solarUsedW = {}, remainingLoadW = {}",
                solarUsedW,
                remainingLoadW);

        // Grid used second
        if (remainingLoadW > 0) {
            gridFlowW = remainingLoadW; // import
            remainingLoadW = 0;

            log.debug(
                    "processEnergyBalance: grid import applied, gridFlowW = {}",
                    gridFlowW);
        }

        // Battery used last (only if load still not satisfied)
        if (remainingLoadW > 0) {
            double maxDischargeW = Math.min(remainingLoadW, 3000.0);
            double dischargedW = calculateBatteryDischarge(meter, maxDischargeW);
            batteryFlowW = -dischargedW;
            remainingLoadW -= dischargedW;

            log.debug(
                    "processEnergyBalance: battery discharge applied, dischargedW = {}, remainingLoadW = {}",
                    dischargedW,
                    remainingLoadW);
        }

        // Surplus handling (solar excess)
        double surplusW = solarW - solarUsedW;

        log.debug(
                "processEnergyBalance: surplusW = {}",
                surplusW);

        if (surplusW > 0) {
            // Export to grid first
            gridFlowW -= surplusW; // export

            log.debug(
                    "processEnergyBalance: solar surplus exported to grid, gridFlowW = {}",
                    gridFlowW);

            // Charge battery only if needed
            double maxChargeW = Math.min(surplusW, 3000.0);
            double chargedW = calculateBatteryCharge(meter, maxChargeW);
            batteryFlowW += chargedW;
            gridFlowW += chargedW; // remove charged part from export

            log.debug(
                    "processEnergyBalance: battery charged using surplus, chargedW = {}, batteryFlowW = {}, gridFlowW = {}",
                    chargedW,
                    batteryFlowW,
                    gridFlowW);
        }

        meter.setSolarProductionW(solarW);
        meter.setHomeConsumptionW(loadW);
        meter.setBatteryPowerW(batteryFlowW);
        meter.setGridPowerW(gridFlowW);

        log.debug(
                "processEnergyBalance: final flows | batteryFlowW = {}, gridFlowW = {}",
                batteryFlowW,
                gridFlowW);

        updateEnergyAccumulators(meter, solarW, loadW, gridFlowW);
    }

    public void updateEnergyAccumulators(MeterSnapshot meter, double solarW, double loadW, double gridW) {
        log.debug("updateEnergyAccumulators: updating cumulative energy values");

        double conversionFactor = DELTA_SECONDS / (3600.0 * 1000.0);

        meter.setTotalSolarYieldKwh(
                meter.getTotalSolarYieldKwh() + (solarW * conversionFactor));

        meter.setTotalHomeUsageKwh(
                meter.getTotalHomeUsageKwh() + (loadW * conversionFactor));

        if (gridW > 0) {
            meter.setTotalGridExportKwh(
                    meter.getTotalGridExportKwh() + (gridW * conversionFactor));
            log.debug("updateEnergyAccumulators: grid export accumulated");

        } else if (gridW < 0) {
            meter.setTotalGridImportKwh(
                    meter.getTotalGridImportKwh() + (Math.abs(gridW) * conversionFactor));
            log.debug("updateEnergyAccumulators: grid import accumulated");
        }
    }

    public double calculateBatteryCharge(MeterSnapshot meter, double chargeW) {
        log.debug(
                "calculateBatteryCharge: requested chargeW = {}, batteryRemainingWh = {}",
                chargeW, meter.getBatteryRemainingWh());

        double energyToAddWh = chargeW * DELTA_SECONDS * SECONDS_TO_HOURS;
        double newWh = meter.getBatteryRemainingWh() + energyToAddWh;

        if (newWh >= meter.getBatteryCapacityWh()) {
            double actualAddedWh = meter.getBatteryCapacityWh() - meter.getBatteryRemainingWh();

            meter.setBatteryRemainingWh(meter.getBatteryCapacityWh());
            meter.setChargingStatus(ChargingStatus.FULL);

            log.info("calculateBatteryCharge: battery reached FULL state");

            return actualAddedWh / (DELTA_SECONDS * SECONDS_TO_HOURS);
        } else {
            meter.setBatteryRemainingWh(newWh);
            meter.setChargingStatus(ChargingStatus.CHARGING);

            log.debug(
                    "calculateBatteryCharge: charging, newBatteryWh = {}",
                    newWh);

            return chargeW;
        }
    }

    public double calculateBatteryDischarge(MeterSnapshot meter, double requestedW) {
        log.debug(
                "calculateBatteryDischarge: requestedW = {}, batteryRemainingWh = {}",
                requestedW, meter.getBatteryRemainingWh());

        double energyNeededWh = requestedW * DELTA_SECONDS * SECONDS_TO_HOURS;

        if (meter.getBatteryRemainingWh() >= energyNeededWh) {
            meter.setBatteryRemainingWh(
                    meter.getBatteryRemainingWh() - energyNeededWh);
            meter.setChargingStatus(ChargingStatus.DISCHARGING);

            log.debug("calculateBatteryDischarge: normal discharge");

            return requestedW;
        } else {
            double actualProvidedWh = meter.getBatteryRemainingWh();

            meter.setBatteryRemainingWh(0.0);
            meter.setChargingStatus(ChargingStatus.EMPTY);

            log.info("calculateBatteryDischarge: battery EMPTY");

            return actualProvidedWh / (DELTA_SECONDS * SECONDS_TO_HOURS);
        }
    }
}
