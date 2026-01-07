package com.project.hems.simulator_service_testing.web.controller;

import com.project.hems.simulator_service_testing.model.VirtualSmartMeter;
import com.project.hems.simulator_service_testing.service.MeterSimulationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@Slf4j
@RestController
@RequestMapping("/api/v1/simulation")
@RequiredArgsConstructor
public class MeterController {

    private final MeterSimulationService meterSimulationService;

    @GetMapping("/get-meter-data/{userId}")
    public VirtualSmartMeter getMeterData(@PathVariable Long userId) {
        log.info("get meter data for userId: {}", userId);
        return meterSimulationService.getMeterData(userId);
    }

    @GetMapping("/get-all-meter-data")
    public Collection<VirtualSmartMeter> getAllMeterData() {
        log.info("get meter data");
        return meterSimulationService.getAllMeters();
    }

    @PostMapping("/activate-meter/{userId}")
    public void activateMeterData(@PathVariable Long userId) {
        log.info("activate meter: {}", userId);
        meterSimulationService.activateMeter(userId, 0);
    }
}
