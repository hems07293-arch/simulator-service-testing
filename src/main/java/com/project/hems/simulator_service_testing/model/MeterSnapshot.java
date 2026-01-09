package com.project.hems.simulator_service_testing.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
@JsonIgnoreProperties(ignoreUnknown = true)
public class MeterSnapshot {

    private Long meterId;
    private Long userId;

    @Builder.Default
    private Double currentVoltage = 230.0; // volts

    @Builder.Default
    private Double currentPower = 0.0; // watts (+ charge, - discharge)

    private Double totalEnergyKwh; // cumulative meter energy (grid side)

    private ChargingStatus chargingStatus;

    // Battery (energy-based)
    private Double batteryCapacityWh; // max energy
    private Double batteryRemainingWh; // current energy

    /** Derived, never persisted */
    public Integer getBatterySoc() {
        if (batteryCapacityWh == null || batteryCapacityWh == 0) {
            return 0;
        }
        return (int) Math.round(
                (batteryRemainingWh / batteryCapacityWh) * 100);
    }
}
