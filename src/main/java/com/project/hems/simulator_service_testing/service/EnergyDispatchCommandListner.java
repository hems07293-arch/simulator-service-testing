package com.project.hems.simulator_service_testing.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.project.hems.simulator_service_testing.model.EnergyDispatchCommand;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Setter
public class EnergyDispatchCommandListner {

    @KafkaListener(topics = "${property.config.kafka.energy-dispatch-topic}")
    public void getDispatchCommands(EnergyDispatchCommand command) {
        log.info("getDispatchCommands: got the command = " + command);
        System.out.println("Energy Dispatch Command got from envoy = " + command);
    }
}
