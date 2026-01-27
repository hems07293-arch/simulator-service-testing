package com.project.hems.simulator_service_testing.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SiteCreationConsumer {

    @Autowired
    private MeterManagementService meterManagementService;

    //consume site_id
    @KafkaListener(
            topics = "${property.config.kafka.site-creation-topic}",
            groupId = "${property.config.kafka.site-creation-group-id}")
    public void consumeSiteId(Long id){
        log.info("Site id",id);
        //UID site_id= UUID.fromString(id);
        System.out.println("activate meter is start");
        meterManagementService.activateMeter(id,3000.00);
        System.out.println("activate meter is end");

    }
}
