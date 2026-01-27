package com.project.hems.simulator_service_testing.web.exception;



import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MeterStatusNotFoudException.class)
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public CustomizedErrorResponse handleMeterStatusNotFoudException(MeterStatusNotFoudException ex) {
        return CustomizedErrorResponse.builder()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .error("METER_NOT_FOUND")
                .message(ex.getMessage())
                .build();
    }

    @ExceptionHandler(MeterStatusAlreadyPresentException.class)
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public CustomizedErrorResponse handleMeterStatusAlreadyPresentException(MeterStatusAlreadyPresentException ex) {
        return CustomizedErrorResponse.builder()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .error("METER_ALREADY_PRESENT")
                .message(ex.getMessage())
                .build();
    }

    @ExceptionHandler(InvalidBatteryStatusException.class)
    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    public CustomizedErrorResponse handleInvalidBatteryStatusException(InvalidBatteryStatusException ex) {
        return CustomizedErrorResponse.builder()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .error("INVALID_BATTERY_STATUS")
                .message(ex.getMessage())
                .build();
    }
}
