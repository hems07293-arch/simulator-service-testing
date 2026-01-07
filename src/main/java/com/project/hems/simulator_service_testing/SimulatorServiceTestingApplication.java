package com.project.hems.simulator_service_testing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class SimulatorServiceTestingApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimulatorServiceTestingApplication.class, args);
    }

}
