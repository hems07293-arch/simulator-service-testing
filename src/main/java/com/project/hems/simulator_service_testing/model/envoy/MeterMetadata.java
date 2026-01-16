package com.project.hems.simulator_service_testing.model.envoy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MeterMetadata {
    private Long meterId;
    private Double maxSolarPeakW; // e.g., 5000W
    private Double maxBatteryPowerW; // Max inverter throughput (e.g., 3000W)
    private Double batteryCapacityWh; // Total storage (e.g., 10000Wh)
    private Double gridConnectionLimitW;// Main breaker limit
}