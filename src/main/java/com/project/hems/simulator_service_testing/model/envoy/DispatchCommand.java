package com.project.hems.simulator_service_testing.model.envoy;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DispatchCommand {

    @NotNull
    private String dispatchId;

    @NotNull
    private Long siteId;

    @NotNull
    private Long meterId;

    @NotNull
    private Instant timestamp;

    @NotNull
    private Instant validUntil;

    @NotEmpty
    private List<EnergyPriority> energyPriority;

    @NotNull
    private BatteryControl batteryControl;

    @NotNull
    private GridControl gridControl;

    private String reason;
}
