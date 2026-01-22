package com.project.hems.simulator_service_testing.model;

import java.time.Instant;
import java.util.List;

import com.project.hems.simulator_service_testing.model.envoy.BatteryControl;
import com.project.hems.simulator_service_testing.model.envoy.EnergyPriority;
import com.project.hems.simulator_service_testing.model.envoy.GridControl;

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
public class ActiveControlState {

    private BatteryControl batteryControl;
    private GridControl gridControl;
    private List<EnergyPriority> energyPriorities;

    private Instant validUntil;

    public boolean isActive(Instant now) {
        return validUntil != null && now.isBefore(validUntil);
    }
}
