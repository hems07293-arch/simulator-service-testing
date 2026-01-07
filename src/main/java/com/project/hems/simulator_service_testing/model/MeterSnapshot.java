package com.project.hems.simulator_service_testing.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeterSnapshot {
    private Long userId;
    @Builder.Default()
    private Double currentVoltage = 230.0; // e.g., 230.5
    @Builder.Default()
    private Double currentPower = 0.0; // e.g., 500.0 Watts
    private Double totalEnergyKwh; // e.g., 120.5 kWh (Accumulating)

}