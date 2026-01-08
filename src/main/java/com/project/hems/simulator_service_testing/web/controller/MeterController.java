package com.project.hems.simulator_service_testing.web.controller;

import com.project.hems.simulator_service_testing.model.MeterSnapshot;
import com.project.hems.simulator_service_testing.service.MeterManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/simulation")
@RequiredArgsConstructor
public class MeterController {

    private final MeterManagementService meterManagementService;

    @GetMapping("/get-meter-data/{userId}")
    public ResponseEntity<MeterSnapshot> getMeterData(@PathVariable Long userId) {
        log.info("get meter data for userId: {}", userId);
        return new ResponseEntity<>(meterManagementService.getMeterData(userId), HttpStatus.OK);
    }

    @GetMapping("/get-all-meter-data")
    public Map<String, MeterSnapshot> getAllMeterData() {
        log.info("get meter data");
        return meterManagementService.getAllMeterSnapshot();
    }

    @PostMapping("/activate-meter/{userId}")
    public void activateMeterData(@PathVariable Long userId) {
        log.info("activate meter: {}", userId);
        meterManagementService.activateMeter(userId);
    }
}
