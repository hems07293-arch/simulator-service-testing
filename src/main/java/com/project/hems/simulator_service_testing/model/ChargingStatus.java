package com.project.hems.simulator_service_testing.model;

public enum ChargingStatus {
    FULL,
    CHARGING, // Solar/Grid -> Battery
    DISCHARGING, // Battery -> Home/Grid
    IDLE, // Battery full or disconnected
    EMPTY // SoC at 0%
}