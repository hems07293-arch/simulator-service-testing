package com.project.hems.simulator_service_testing.config;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.project.hems.simulator_service_testing.model.ActiveControlState;

@Component
public class ActiveControlStore {

    // siteId -> active control
    private final ConcurrentHashMap<String, ActiveControlState> activeControls = new ConcurrentHashMap<>();

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
