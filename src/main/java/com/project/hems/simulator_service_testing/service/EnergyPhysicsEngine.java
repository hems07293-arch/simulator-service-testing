package com.project.hems.simulator_service_testing.service;

import org.springframework.stereotype.Component;

import com.project.hems.simulator_service_testing.model.ChargingStatus;
import com.project.hems.simulator_service_testing.model.MeterSnapshot;

@Component
public class EnergyPhysicsEngine {

    private static final double DELTA_SECONDS = 5.0;
    private static final double SECONDS_TO_HOURS = 1.0 / 3600.0;

    public void processEnergyBalance(MeterSnapshot meter, double solarW, double loadW) {
        double netPowerW = solarW - loadW;
        double batteryFlowW = 0.0;

        if (netPowerW > 0) {
            // Surplus logic
            double maxChargePossibleW = Math.min(netPowerW, 3000.0);
            batteryFlowW = calculateBatteryCharge(meter, maxChargePossibleW);
        } else if (netPowerW < 0) {
            // Deficit logic
            double deficitW = Math.abs(netPowerW);
            double maxDischargePossibleW = Math.min(deficitW, 3000.0);
            batteryFlowW = -calculateBatteryDischarge(meter, maxDischargePossibleW);
        } else {
            meter.setChargingStatus(ChargingStatus.IDLE);
        }

        // Grid Balance Calculation
        double gridW = (solarW - loadW) - batteryFlowW;

        meter.setSolarProductionW(solarW);
        meter.setHomeConsumptionW(loadW);
        meter.setBatteryPowerW(batteryFlowW);
        meter.setGridPowerW(gridW);

        updateEnergyAccumulators(meter, solarW, loadW, gridW);
    }

    public void updateEnergyAccumulators(MeterSnapshot meter, double solarW, double loadW, double gridW) {
        // Convert Watts to kWh for this 5-second tick
        // (W * seconds) / (3600 seconds/hour * 1000 W/kW)
        double conversionFactor = DELTA_SECONDS / (3600.0 * 1000.0);

        // 1. Accumulate Solar Yield
        meter.setTotalSolarYieldKwh(meter.getTotalSolarYieldKwh() + (solarW * conversionFactor));

        // 2. Accumulate Home Usage
        meter.setTotalHomeUsageKwh(meter.getTotalHomeUsageKwh() + (loadW * conversionFactor));

        // 3. Accumulate Grid Import/Export
        if (gridW > 0) {
            // We are exporting (Selling to grid)
            meter.setTotalGridExportKwh(meter.getTotalGridExportKwh() + (gridW * conversionFactor));
        } else if (gridW < 0) {
            // We are importing (Buying from grid)
            meter.setTotalGridImportKwh(meter.getTotalGridImportKwh() + (Math.abs(gridW) * conversionFactor));
        }
    }

    public double calculateBatteryCharge(MeterSnapshot meter, double chargeW) {
        double energyToAddWh = chargeW * DELTA_SECONDS * SECONDS_TO_HOURS;
        double newWh = meter.getBatteryRemainingWh() + energyToAddWh;

        if (newWh >= meter.getBatteryCapacityWh()) {
            double actualAddedWh = meter.getBatteryCapacityWh() - meter.getBatteryRemainingWh();
            meter.setBatteryRemainingWh(meter.getBatteryCapacityWh());
            meter.setChargingStatus(ChargingStatus.FULL);
            return (actualAddedWh / (DELTA_SECONDS * SECONDS_TO_HOURS)); // Actual W used
        } else {
            meter.setBatteryRemainingWh(newWh);
            meter.setChargingStatus(ChargingStatus.CHARGING);
            return chargeW;
        }
    }

    public double calculateBatteryDischarge(MeterSnapshot meter, double requestedW) {
        // Convert the requested Power (W) to Energy (Wh) for this time slice
        double energyNeededWh = requestedW * DELTA_SECONDS * SECONDS_TO_HOURS;

        // Check if battery has enough energy
        if (meter.getBatteryRemainingWh() >= energyNeededWh) {
            // Normal Case: Battery handles the full request
            meter.setBatteryRemainingWh(meter.getBatteryRemainingWh() - energyNeededWh);
            meter.setChargingStatus(ChargingStatus.DISCHARGING);
            return requestedW;
        } else {
            // Corner Case: Battery is almost empty, take what's left
            double actualProvidedWh = meter.getBatteryRemainingWh();
            meter.setBatteryRemainingWh(0.0);
            meter.setChargingStatus(ChargingStatus.EMPTY);

            // Convert the actual Wh pulled back into Watts (W) to maintain balance
            return (actualProvidedWh / (DELTA_SECONDS * SECONDS_TO_HOURS));
        }
    }
}
