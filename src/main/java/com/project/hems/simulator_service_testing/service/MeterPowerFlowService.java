package com.project.hems.simulator_service_testing.service;

import com.project.hems.simulator_service_testing.model.BatteryMode;
import com.project.hems.simulator_service_testing.model.ChargingStatus;
import com.project.hems.simulator_service_testing.model.MeterSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import org.springframework.stereotype.Service;

@Deprecated
@Slf4j
@Service
@RequiredArgsConstructor
public class MeterPowerFlowService {

    private final Map<String, MeterSnapshot> meterReadings;

    public void startDispatchingPower(Long siteId) {

        log.info("startDispatchingPower: Power dispatch requested for siteId={}", siteId);

        // Fetch current snapshot without deleting it (safer & atomic update below)

        MeterSnapshot meterSnapshot = meterReadings.get(siteId.toString());

        if (meterSnapshot == null) {
            log.warn("startDispatchingPower: No meter snapshot found in Bean Map for siteId={}", siteId);
            return;
        }

        // Idempotent state transition: only switch if not already discharging
        if (meterSnapshot.getChargingStatus() == ChargingStatus.DISCHARGING) {
            log.debug("startDispatchingPower: Meter already in DISCHARGING state [siteId={}]", siteId);
            return;
        }

        log.info(
                "startDispatchingPower: Switching meter to DISCHARGING state [siteId={}, previousStatus={}]",
                siteId,
                meterSnapshot.getChargingStatus());

        // Update battery flow direction
        meterSnapshot.setChargingStatus(ChargingStatus.DISCHARGING);

        // Persist updated snapshot back to Bean Map
        meterReadings.put(siteId.toString(), meterSnapshot);

        log.info("startDispatchingPower: Power dispatch started successfully for siteId={}", siteId);
    }

    public void stopDispatchingPower(Long siteId) {
        log.info("stopDispatchingPower: Power dispatch requested for siteId={}", siteId);

        // Fetch current snapshot without deleting it (safer & atomic update below)
        MeterSnapshot meterSnapshot = meterReadings.get(siteId.toString());

        if (meterSnapshot == null) {
            log.warn("stopDispatchingPower: No meter snapshot found in Bean Map for siteId={}", siteId);
            return;
        }

        // Idempotent state transition: only switch if not already charging
        if (meterSnapshot.getChargingStatus() == ChargingStatus.CHARGING) {
            log.debug("stopDispatchingPower: Meter already in CHARGING state [siteId={}]", siteId);
            return;
        }

        log.info(
                "stopDispatchingPower: Switching meter to DISCHARGING state [siteId={}, previousStatus={}]",
                siteId,
                meterSnapshot.getChargingStatus());

        // Update battery flow direction
        meterSnapshot.setChargingStatus(ChargingStatus.CHARGING);

        // Persist updated snapshot back to Bean Map
        meterReadings.put(siteId.toString(), meterSnapshot);

        log.info("stopDispatchingPower: Power dispatch started successfully for siteId={}", siteId);
    }

    public void changeBatteryMode(Long siteId, BatteryMode batteryMode) {
        MeterSnapshot meterSnapshot = meterReadings.get(siteId.toString());

        if (meterSnapshot == null) {
            log.error("error geting meter detail for given site id " + siteId);
            return;
        }

        meterSnapshot.setBatteryMode(batteryMode);

        meterReadings.put(siteId.toString(), meterSnapshot);
    }
}
