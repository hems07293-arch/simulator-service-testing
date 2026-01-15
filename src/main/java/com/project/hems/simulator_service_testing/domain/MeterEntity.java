package com.project.hems.simulator_service_testing.domain;

import java.sql.Timestamp;
import java.sql.Types;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;

import com.project.hems.simulator_service_testing.model.BatteryMode;
import com.project.hems.simulator_service_testing.model.ChargingStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "meter_info")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class MeterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long siteId;

    // --- Cumulative Energy Accumulators (kWh) ---
    // Using precision (15,4) to prevent rounding errors in energy accounting

    @Builder.Default
    @Column(precision = 15, scale = 4)
    @JdbcTypeCode(Types.DECIMAL)
    private Double totalSolarYieldKwh = 0.0;

    @Builder.Default
    @Column(precision = 15, scale = 4)
    @JdbcTypeCode(Types.DECIMAL)
    private Double totalGridImportKwh = 0.0;

    @Builder.Default
    @Column(precision = 15, scale = 4)
    @JdbcTypeCode(Types.DECIMAL)
    private Double totalGridExportKwh = 0.0;

    @Builder.Default
    @Column(precision = 15, scale = 4)
    @JdbcTypeCode(Types.DECIMAL)
    private Double totalHomeUsageKwh = 0.0;

    // --- Battery Configuration & State ---

    @Enumerated(EnumType.STRING)
    private ChargingStatus chargingStatus;

    @Enumerated(EnumType.STRING)
    private BatteryMode batteryMode; // Saved state (AUTO, VPP_DISPATCH, etc.)

    private Double batteryCapacityWh;
    private Double batteryRemainingWh;

    // Derived value for quick DB queries, though usually calculated in the POJO
    private Integer batterySoc;

    // --- Audit Metadata ---

    @CreationTimestamp
    @Column(updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp lastUpdatedAt;
}