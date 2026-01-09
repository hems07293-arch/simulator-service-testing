package com.project.hems.simulator_service_testing.service;

import com.project.hems.simulator_service_testing.config.SimulationRedisProperties;
import com.project.hems.simulator_service_testing.model.ChargingStatus;
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

        log.info("startDispatchingPower: Power dispatch requested for userId={}", userId);

        // Construct Redis key for this meter
        String redisKey = simulationRedisProperties.getREDIS_KEY() + userId;

        log.debug("startDispatchingPower: Fetching meter snapshot from Redis [key={}]", redisKey);

        // Fetch current snapshot without deleting it (safer & atomic update below)
        MeterSnapshot meterSnapshot = redisTemplate.opsForValue().get(redisKey);

        if (meterSnapshot == null) {
            log.warn("startDispatchingPower: No meter snapshot found in Redis for userId={}", userId);
            return;
        }

        // Idempotent state transition: only switch if not already discharging
        if (meterSnapshot.getChargingStatus() == ChargingStatus.DISCHARGING) {
            log.debug("startDispatchingPower: Meter already in DISCHARGING state [userId={}]", userId);
            return;
        }

        log.info(
                "startDispatchingPower: Switching meter to DISCHARGING state [userId={}, previousStatus={}]",
                userId,
                meterSnapshot.getChargingStatus());

        // Update battery flow direction
        meterSnapshot.setChargingStatus(ChargingStatus.DISCHARGING);

        // Persist updated snapshot back to Redis
        redisTemplate.opsForValue().set(redisKey, meterSnapshot);

        log.info("startDispatchingPower: Power dispatch started successfully for userId={}", userId);
    }

    public void stopDispatchingPower(Long userId) {
        log.info("stopDispatchingPower: Power dispatch requested for userId={}", userId);

        // Construct Redis key for this meter
        String redisKey = simulationRedisProperties.getREDIS_KEY() + userId;

        log.debug("stopDispatchingPower: Fetching meter snapshot from Redis [key={}]", redisKey);

        // Fetch current snapshot without deleting it (safer & atomic update below)
        MeterSnapshot meterSnapshot = redisTemplate.opsForValue().get(redisKey);

        if (meterSnapshot == null) {
            log.warn("stopDispatchingPower: No meter snapshot found in Redis for userId={}", userId);
            return;
        }

        // Idempotent state transition: only switch if not already charging
        if (meterSnapshot.getChargingStatus() == ChargingStatus.CHARGING) {
            log.debug("stopDispatchingPower: Meter already in CHARGING state [userId={}]", userId);
            return;
        }

        log.info(
                "stopDispatchingPower: Switching meter to DISCHARGING state [userId={}, previousStatus={}]",
                userId,
                meterSnapshot.getChargingStatus());

        // Update battery flow direction
        meterSnapshot.setChargingStatus(ChargingStatus.DISCHARGING);

        // Persist updated snapshot back to Redis
        redisTemplate.opsForValue().set(redisKey, meterSnapshot);

        log.info("stopDispatchingPower: Power dispatch started successfully for userId={}", userId);
    }
}
