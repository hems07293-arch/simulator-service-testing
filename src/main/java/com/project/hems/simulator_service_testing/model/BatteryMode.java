package com.project.hems.simulator_service_testing.model;

public enum BatteryMode {
    AUTO, // Your "Priority Decision Matrix" logic
    FORCE_DISCHARGE, // VPP Request: Dump battery to grid
    FORCE_CHARGE, // VPP Request: Charge from grid immediately
    ECO_MODE // Save battery for specific hours
}
