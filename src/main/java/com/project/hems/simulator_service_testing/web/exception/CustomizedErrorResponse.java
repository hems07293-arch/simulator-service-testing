package com.project.hems.simulator_service_testing.web.exception;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CustomizedErrorResponse {
    private int statusCode;
    private String error;
    private String message;
}
