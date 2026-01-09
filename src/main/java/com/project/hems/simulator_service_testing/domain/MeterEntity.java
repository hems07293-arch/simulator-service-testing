package com.project.hems.simulator_service_testing.domain;

import com.project.hems.simulator_service_testing.model.ChargingStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "meter_info")
@NoArgsConstructor
@Data
public class MeterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    // Meter-side energy (grid)
    private Double lastKnownEnergyKwh;

    @Enumerated(EnumType.STRING)
    private ChargingStatus chargingStatus;

    // Battery (energy-based)
    private Double batteryCapacityWh;
    private Double batteryRemainingWh;
    private Integer batterySoc;
}
