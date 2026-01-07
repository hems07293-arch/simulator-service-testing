package com.project.hems.simulator_service_testing.web.exception;

public class MeterStatusAlreadyPresentException extends RuntimeException {

    public MeterStatusAlreadyPresentException(String msg) {
        super(msg);
    }
}
