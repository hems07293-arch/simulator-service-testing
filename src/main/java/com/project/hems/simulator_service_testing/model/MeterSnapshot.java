package com.project.hems.simulator_service_testing.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.project.hems.simulator_service_testing.model.envoy.EnergyPriority;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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

    // @NotNull(message = "meterId cannot be null")
    private Long meterId;

    @NotNull(message = "siteId cannot be null")
    private Long siteId;

    @NotNull(message = "timestamp cannot be null")
    private LocalDateTime timestamp;

    // --- 1. Real-Time Power Flow (Watts) ---

    @NotNull
    @PositiveOrZero(message = "solarProductionW cannot be negative")
    private Double solarProductionW;

    @NotNull
    @PositiveOrZero(message = "homeConsumptionW cannot be negative")
    private Double homeConsumptionW;

    @NotNull(message = "batteryPowerW cannot be null")
    private Double batteryPowerW; // can be + or -

    @NotNull(message = "gridPowerW cannot be null")
    private Double gridPowerW; // can be + or -

    // --- 2. Energy Accumulators (kWh) ---

    @NotNull
    @PositiveOrZero(message = "totalSolarYieldKwh cannot be negative")
    private Double totalSolarYieldKwh;

    @NotNull
    @PositiveOrZero(message = "totalGridImportKwh cannot be negative")
    private Double totalGridImportKwh;

    @NotNull
    @PositiveOrZero(message = "totalGridExportKwh cannot be negative")
    private Double totalGridExportKwh;

    @NotNull
    @PositiveOrZero(message = "totalHomeUsageKwh cannot be negative")
    private Double totalHomeUsageKwh;

    // --- 3. Battery State ---

    @NotNull
    @Positive(message = "batteryCapacityWh must be greater than 0")
    private Double batteryCapacityWh;

    @NotNull
    @PositiveOrZero(message = "batteryRemainingWh cannot be negative")
    private Double batteryRemainingWh;

    @NotNull(message = "chargingStatus cannot be null")
    private ChargingStatus chargingStatus;

    @NotNull(message = "batteryMode cannot be null")
    private BatteryMode batteryMode;

    // --- 4. Electrical Metadata ---

    @NotNull
    @Positive(message = "currentVoltage must be greater than 0")
    private Double currentVoltage;

    @NotNull
    @PositiveOrZero(message = "currentAmps cannot be negative")
    private Double currentAmps;

    @NotNull
    @Min(value = 0, message = "batterySoc cannot be less than 0")
    @Max(value = 100, message = "batterySoc cannot be greater than 100")
    private Integer batterySoc;

    @NotNull
    @NotEmpty(message = "energy priority is required for simulation")
    private List<EnergyPriority> energyPriorities;
}
