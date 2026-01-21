package com.project.hems.simulator_service_testing.web.controller;

import com.project.hems.simulator_service_testing.model.MeterSnapshot;
import com.project.hems.simulator_service_testing.model.envoy.EnergyPriority;
import com.project.hems.simulator_service_testing.service.MeterManagementService;
import com.project.hems.simulator_service_testing.service.MeterPowerFlowService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/simulation")
@RequiredArgsConstructor
public class MeterController {

    private final MeterManagementService meterManagementService;
    private final MeterPowerFlowService meterPowerFlowService;
    private final Map<String, MeterSnapshot> meterReadings;

    @GetMapping("/get-meter-data/{userId}")
    public ResponseEntity<MeterSnapshot> getMeterData(@PathVariable Long userId) {
        log.info("get meter data for userId: {}", userId);
        return new ResponseEntity<>(meterManagementService.getMeterData(userId), HttpStatus.OK);
    }

    @GetMapping("/get-all-meter-data")
    public Map<String, MeterSnapshot> getAllMeterData() {
        log.info("get meter data");
        return meterReadings;
    }

    @PostMapping("/activate-meter/{siteId}")
    public void activateMeterData(@PathVariable Long siteId, @RequestBody Double batteryCapacity) {
        log.info("activate meter: {}", siteId, batteryCapacity);
        meterManagementService.activateMeter(siteId, batteryCapacity);
    }

    @PutMapping("/start-dispatching/{siteId}")
    public void startDispatchingEnergy(@PathVariable Long siteId) {
        log.info("dispatching power from meter: {}", siteId);
        meterPowerFlowService.startDispatchingPower(siteId);
    }

    @PutMapping("/stop-dispatching/{siteId}")
    public void stopDispatchingEnergy(@PathVariable Long siteId) {
        log.info("stop dispatching power from meter: {}", siteId);
        meterPowerFlowService.stopDispatchingPower(siteId);
    }

    @PostMapping("/change-priority/{siteId}")
    public void postMethodName(@PathVariable Long siteId, @RequestBody List<EnergyPriority> energyPriorities) {
        log.info("changing priority of energy flow");
        energyPriorities.forEach(e -> System.out.println(e));

    }

}
