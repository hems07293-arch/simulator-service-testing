package com.project.hems.simulator_service_testing.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.project.hems.simulator_service_testing.model.ActiveControlState;
import com.project.hems.simulator_service_testing.model.BatteryMode;
import com.project.hems.simulator_service_testing.model.ChargingStatus;
import com.project.hems.simulator_service_testing.model.MeterSnapshot;
import com.project.hems.simulator_service_testing.model.envoy.BatteryControl;
import com.project.hems.simulator_service_testing.model.envoy.EnergyPriority;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class EnergyPhysicsEngine {

        private static final double DELTA_SECONDS = 5.0;
        private static final double SECONDS_TO_HOURS = 1.0 / 3600.0;

        private boolean isGridImportAllowed(ActiveControlState control) {
                return control == null
                                || control.getGridControl() == null
                                || control.getGridControl().getAllowImport();
        }

        private boolean isGridExportAllowed(ActiveControlState control) {
                return control == null
                                || control.getGridControl() == null
                                || control.getGridControl().getAllowExport();
        }

        public void processEnergyBalance(
                        MeterSnapshot meter,
                        double solarW,
                        double loadW,
                        List<EnergyPriority> priorityOrder,
                        @Nullable ActiveControlState control) {

                double remainingLoadW = loadW;
                double remainingSolarW = solarW;

                double batteryFlowW = 0.0;
                double gridFlowW = 0.0;

                // 1. Serve LOAD by priority
                for (EnergyPriority priority : priorityOrder) {

                        if (remainingLoadW <= 0)
                                break;

                        switch (priority) {

                                case SOLAR -> {
                                        double used = Math.min(remainingSolarW, remainingLoadW);
                                        remainingSolarW -= used;
                                        remainingLoadW -= used;
                                }

                                case GRID -> {
                                        if (isGridImportAllowed(control)) {
                                                gridFlowW += remainingLoadW;
                                                remainingLoadW = 0;
                                        }
                                }

                                case BATTERY -> {

                                        double allowedDischargeW = Math.min(
                                                        remainingLoadW,
                                                        getMaxDischargeW(meter, control));

                                        if (allowedDischargeW > 0) {
                                                double dischargedW = calculateBatteryDischarge(meter,
                                                                allowedDischargeW);
                                                batteryFlowW -= dischargedW;
                                                remainingLoadW -= dischargedW;
                                        }
                                }

                        }
                }

                // 2. Handle SURPLUS by priority
                double surplusW = remainingSolarW;

                if (surplusW > 0) {
                        for (EnergyPriority priority : priorityOrder) {

                                if (surplusW <= 0)
                                        break;

                                switch (priority) {

                                        case BATTERY -> {

                                                double allowedChargeW = Math.min(
                                                                surplusW,
                                                                getMaxChargeW(meter, control));

                                                if (allowedChargeW > 0) {
                                                        double chargedW = calculateBatteryCharge(meter, allowedChargeW);
                                                        batteryFlowW += chargedW;
                                                        surplusW -= chargedW;
                                                }
                                        }

                                        case GRID -> {
                                                if (isGridExportAllowed(control)) {
                                                        gridFlowW -= surplusW;
                                                        surplusW = 0;
                                                }
                                        }

                                        case SOLAR -> {
                                                // Solar cannot absorb surplus
                                        }
                                }
                        }
                }

                // 3. Persist flows
                meter.setSolarProductionW(solarW);
                meter.setHomeConsumptionW(loadW);
                meter.setBatteryPowerW(batteryFlowW);
                meter.setGridPowerW(gridFlowW);

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

        private double getMaxDischargeW(MeterSnapshot meter, ActiveControlState control) {

                double defaultMax = 3000.0;

                if (control == null || control.getBatteryControl() == null) {
                        return defaultMax;
                }

                BatteryControl bc = control.getBatteryControl();

                if (bc.getMode() == BatteryMode.FORCE_CHARGE) {
                        return 0; // discharge forbidden
                }

                if (bc.getMaxDischargeW() != null) {
                        defaultMax = bc.getMaxDischargeW();
                }

                if (meter.getBatterySoc() <= bc.getMinSocPercent()) {
                        return 0;
                }

                return defaultMax;
        }

        private double getMaxChargeW(MeterSnapshot meter, ActiveControlState control) {

                double defaultMax = 3000.0;

                if (control == null || control.getBatteryControl() == null) {
                        return defaultMax;
                }

                BatteryControl bc = control.getBatteryControl();

                if (bc.getMode() == BatteryMode.FORCE_DISCHARGE) {
                        return 0; // charge forbidden
                }

                if (bc.getMaxChargeW() != null) {
                        defaultMax = bc.getMaxChargeW();
                }

                if (meter.getBatterySoc() >= bc.getMaxSocPercent()) {
                        return 0;
                }

                return defaultMax;
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
