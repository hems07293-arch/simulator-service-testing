package com.project.hems.simulator_service_testing.model;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
// This represents one user's Smart Meter in memory
public class VirtualSmartMeter {
    private Long userId;
    private Double currentVoltage; // e.g., 230.5
    private Double currentPower; // e.g., 500.0 Watts
    private Double totalEnergyKwh; // e.g., 120.5 kWh (Accumulating)

    public VirtualSmartMeter(Long userId, Double startKwh) {
        this.userId = userId;
        this.totalEnergyKwh = startKwh;
        this.currentVoltage = 230.0;
        this.currentPower = 0.0;
    }
}