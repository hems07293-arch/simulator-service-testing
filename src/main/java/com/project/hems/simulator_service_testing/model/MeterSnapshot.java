package com.project.hems.simulator_service_testing.model;

import java.io.Serializable;
import java.time.LocalDateTime;

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
public class MeterSnapshot implements Serializable {

    private Long meterId;
    private Long siteId;
    private LocalDateTime timestamp;

    // --- 1. Real-Time Power Flow (Watts) ---
    private Double solarProductionW; // Logic-driven (Sine wave)
    private Double homeConsumptionW; // Logic-driven (Base + Spikes)
    private Double batteryPowerW; // (+) Charging, (-) Discharging
    private Double gridPowerW; // (+) Exporting, (-) Importing

    // --- 2. Energy Accumulators (kWh) ---
    // Persistent values for the "Cold Storage" DB
    private Double totalSolarYieldKwh;
    private Double totalGridImportKwh;
    private Double totalGridExportKwh;
    private Double totalHomeUsageKwh;

    // --- 3. Battery State ---
    private Double batteryCapacityWh;
    private Double batteryRemainingWh;
    private ChargingStatus chargingStatus;
    private BatteryMode batteryMode; // Critical for VPP Logic

    // --- 4. Electrical Metadata ---
    private Double currentVoltage; // Fixes the "undefined" error
    private Double currentAmps;
    private Integer batterySoc;
}
