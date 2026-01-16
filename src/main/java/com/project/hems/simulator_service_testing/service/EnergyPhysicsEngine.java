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
        log.debug("processEnergyBalance: start energy balance calculation");

        double netPowerW = solarW - loadW;
        double batteryFlowW = 0.0;

        log.debug(
                "processEnergyBalance: solarW = {}, loadW = {}, netPowerW = {}",
                solarW, loadW, netPowerW);

        if (netPowerW > 0) {
            double maxChargePossibleW = Math.min(netPowerW, 3000.0);
            log.debug(
                    "processEnergyBalance: surplus detected, maxChargePossibleW = {}",
                    maxChargePossibleW);
            batteryFlowW = calculateBatteryCharge(meter, maxChargePossibleW);

        } else if (netPowerW < 0) {
            double deficitW = Math.abs(netPowerW);
            double maxDischargePossibleW = Math.min(deficitW, 3000.0);
            log.debug(
                    "processEnergyBalance: deficit detected, maxDischargePossibleW = {}",
                    maxDischargePossibleW);
            batteryFlowW = -calculateBatteryDischarge(meter, maxDischargePossibleW);

        } else {
            log.debug("processEnergyBalance: perfect balance, battery idle");
            meter.setChargingStatus(ChargingStatus.IDLE);
        }

        double gridW = (solarW - loadW) - batteryFlowW;

        log.debug(
                "processEnergyBalance: batteryFlowW = {}, gridW = {}",
                batteryFlowW, gridW);

        meter.setSolarProductionW(solarW);
        meter.setHomeConsumptionW(loadW);
        meter.setBatteryPowerW(batteryFlowW);
        meter.setGridPowerW(gridW);

        updateEnergyAccumulators(meter, solarW, loadW, gridW);
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
