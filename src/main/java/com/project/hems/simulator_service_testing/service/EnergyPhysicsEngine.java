package com.project.hems.simulator_service_testing.service;


import com.project.hems.simulator_service_testing.model.ActiveControlState;
import com.project.hems.simulator_service_testing.model.BatteryMode;
import com.project.hems.simulator_service_testing.model.ChargingStatus;
import com.project.hems.simulator_service_testing.model.MeterSnapshot;
import com.project.hems.simulator_service_testing.model.envoy.BatteryControl;
import com.project.hems.simulator_service_testing.model.envoy.EnergyPriority;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

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

                log.info(
                                "EnergyBalance START | siteId={} meterId={} solarW={} loadW={} priority={} control={}",
                                meter.getSiteId(),
                                meter.getMeterId(),
                                solarW,
                                loadW,
                                priorityOrder,
                                control != null ? control.getBatteryControl() : "NONE");

                double remainingLoadW = loadW;
                double remainingSolarW = solarW;

                double batteryFlowW = 0.0;
                double gridFlowW = 0.0;

                // 1. Serve LOAD by priority
                for (EnergyPriority priority : priorityOrder) {

                        if (remainingLoadW <= 0) {
                                log.debug("Load fully served, skipping remaining priorities");
                                break;
                        }

                        switch (priority) {

                                case SOLAR -> {
                                        double used = Math.min(remainingSolarW, remainingLoadW);
                                        remainingSolarW -= used;
                                        remainingLoadW -= used;

                                        log.debug(
                                                        "LOAD ← SOLAR | used={}W remainingLoad={}W remainingSolar={}W",
                                                        used, remainingLoadW, remainingSolarW);
                                }

                                case GRID -> {
                                        if (isGridImportAllowed(control)) {
                                                log.debug("LOAD ← GRID | importing {}W", remainingLoadW);
                                                gridFlowW += remainingLoadW;
                                                remainingLoadW = 0;
                                        } else {
                                                log.warn("GRID import BLOCKED by control");
                                        }
                                }

                                case BATTERY -> {
                                        double maxAllowed = getMaxDischargeW(meter, control);
                                        double allowedDischargeW = Math.min(remainingLoadW, maxAllowed);

                                        log.debug(
                                                        "LOAD ← BATTERY | requested={}W allowed={}W SOC={}%",
                                                        remainingLoadW,
                                                        allowedDischargeW,
                                                        meter.getBatterySoc());

                                        if (allowedDischargeW > 0) {
                                                double dischargedW = calculateBatteryDischarge(meter,
                                                                allowedDischargeW);
                                                batteryFlowW -= dischargedW;
                                                remainingLoadW -= dischargedW;

                                                log.debug(
                                                                "BATTERY discharged={}W remainingLoad={}W",
                                                                dischargedW, remainingLoadW);
                                        } else {
                                                log.warn("BATTERY discharge BLOCKED (SOC/control)");
                                        }
                                }
                        }
                }

                // 2. Handle SURPLUS by priority
                double surplusW = remainingSolarW;
                log.debug("SURPLUS detected={}W", surplusW);

                if (surplusW > 0) {
                        for (EnergyPriority priority : priorityOrder) {

                                if (surplusW <= 0) {
                                        log.debug("Surplus fully handled");
                                        break;
                                }

                                switch (priority) {

                                        case BATTERY -> {
                                                double maxAllowed = getMaxChargeW(meter, control);
                                                double allowedChargeW = Math.min(surplusW, maxAllowed);

                                                log.debug(
                                                                "SURPLUS → BATTERY | requested={}W allowed={}W SOC={}%",
                                                                surplusW,
                                                                allowedChargeW,
                                                                meter.getBatterySoc());

                                                if (allowedChargeW > 0) {
                                                        double chargedW = calculateBatteryCharge(meter, allowedChargeW);
                                                        batteryFlowW += chargedW;
                                                        surplusW -= chargedW;

                                                        log.debug(
                                                                        "BATTERY charged={}W remainingSurplus={}W",
                                                                        chargedW, surplusW);
                                                } else {
                                                        log.warn("BATTERY charge BLOCKED (SOC/control)");
                                                }
                                        }

                                        case GRID -> {
                                                if (isGridExportAllowed(control)) {
                                                        log.debug("SURPLUS → GRID | exporting {}W", surplusW);
                                                        gridFlowW -= surplusW;
                                                        surplusW = 0;
                                                } else {
                                                        log.warn("GRID export BLOCKED by control");
                                                }
                                        }

                                        case SOLAR -> {
                                                log.debug("SURPLUS → SOLAR skipped (no sink)");
                                        }
                                }
                        }
                }

                // 3. Persist flows
                meter.setSolarProductionW(solarW);
                meter.setHomeConsumptionW(loadW);
                meter.setBatteryPowerW(batteryFlowW);
                meter.setGridPowerW(gridFlowW);

                // 4. Battery status
                meter.setChargingStatus(deriveChargingStatus(batteryFlowW));

                updateEnergyAccumulators(meter, solarW, loadW, gridFlowW);

                log.info(
                                "EnergyBalance END | batteryFlowW={} gridFlowW={} status={} remainingSolar={} remainingLoad={}",
                                batteryFlowW,
                                gridFlowW,
                                deriveChargingStatus(batteryFlowW),
                                remainingSolarW,
                                remainingLoadW);

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

        private ChargingStatus deriveChargingStatus(double batteryFlowW) {

                if (batteryFlowW > 0.01) {
                        return ChargingStatus.CHARGING;
                }

                if (batteryFlowW < -0.01) {
                        return ChargingStatus.DISCHARGING;
                }

                return ChargingStatus.IDLE;
        }

        private double getMaxDischargeW(MeterSnapshot meter, ActiveControlState control) {
                log.debug(
                                "MaxDischarge check | mode={} maxDischargeW={} SOC={} minSOC={}",
                                control.getBatteryControl().getMode(),
                                control.getBatteryControl().getMaxDischargeW(),
                                meter.getBatterySoc(),
                                control.getBatteryControl().getMinSocPercent());

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

                log.debug(
                                "MaxCharge check | mode={} maxChargeW={} SOC={} maxSOC={}",
                                control.getBatteryControl().getMode(),
                                control.getBatteryControl().getMaxChargeW(),
                                meter.getBatterySoc(),
                                control.getBatteryControl().getMaxSocPercent());

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
