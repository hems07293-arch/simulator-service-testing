package com.project.hems.simulator_service_testing.config;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.project.hems.simulator_service_testing.model.ActiveControlState;
import com.project.hems.simulator_service_testing.model.envoy.EnergyPriority;

@Component
public class ActiveControlStore {

    // siteId -> active control
    private final ConcurrentHashMap<String, ActiveControlState> activeControls = new ConcurrentHashMap<>();
    public static final List<EnergyPriority> energyPriorities = List.of(EnergyPriority.SOLAR, EnergyPriority.GRID,
            EnergyPriority.BATTERY);

    public void applyDispatch(String siteId, ActiveControlState control) {
        activeControls.put(siteId, control);
    }

    public Optional<ActiveControlState> getActiveControl(String siteId) {
        ActiveControlState control = activeControls.get(siteId);

        if (control == null || !control.isActive(Instant.now())) {
            activeControls.remove(siteId);
            return Optional.empty();
        }

        return Optional.of(control);
    }
}
