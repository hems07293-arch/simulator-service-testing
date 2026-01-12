package com.project.hems.simulator_service_testing.web.exception;

public class InvalidBatteryStatusException extends RuntimeException {
    public InvalidBatteryStatusException(String msg) {
        super(msg);
    }
}
