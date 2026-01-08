package com.project.hems.simulator_service_testing.service;

import com.project.hems.simulator_service_testing.config.SimulationRedisProperties;
import com.project.hems.simulator_service_testing.model.MeterSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeterPowerFlowService {

    private final RedisTemplate<String, MeterSnapshot> redisTemplate;
    private final SimulationRedisProperties simulationRedisProperties;

    public void startDispatchingPower(Long userId) {
        log.info("startDispatchingPower: userId={}", userId);

        log.debug("deleing the meter value from redis");
        MeterSnapshot deletedValue = redisTemplate.opsForValue().getAndDelete(simulationRedisProperties.getREDIS_KEY() + userId.toString());
        log.info("startDispatchingPower: deletedValue={}", deletedValue);

//        Dispatching Logic

    }

    public void stopDispatchingPower(Long userId) {
        log.info("stopDispatchingPower: userId={}", userId);
    }

    public void startChargingPower(Long userId) {
        log.info("startChargingPower: userId={}", userId);
    }

    public void stopChargingPower(Long userId) {
        log.info("stopChargingPower: userId={}", userId);
    }
}
