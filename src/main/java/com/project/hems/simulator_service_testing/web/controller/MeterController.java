package com.project.hems.simulator_service_testing.web.controller;

import com.project.hems.simulator_service_testing.model.MeterSnapshot;
import com.project.hems.simulator_service_testing.service.MeterRedisService;
import com.project.hems.simulator_service_testing.service.MeterSimulationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/simulation")
@RequiredArgsConstructor
public class MeterController {

    private final MeterRedisService meterRedisService;
    private final MeterSimulationService meterSimulationService;

    @GetMapping("/get-meter-data/{userId}")
    public ResponseEntity<MeterSnapshot> getMeterData(@PathVariable Long userId) {
        log.info("get meter data for userId: {}", userId);
        return new ResponseEntity<>(meterRedisService.getSnapshot(userId), HttpStatus.OK);
    }

    // @GetMapping("/get-all-meter-data")
    // public Collection<MeterSnapshot> getAllMeterData() {
    // log.info("get meter data");
    // return meterRedisService.getSnapshot();
    // }

    @PostMapping("/activate-meter/{userId}")
    public void activateMeterData(@PathVariable Long userId) {
        log.info("activate meter: {}", userId);
        meterSimulationService.activateMeter(userId);
    }
}
