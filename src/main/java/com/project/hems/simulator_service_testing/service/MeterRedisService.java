package com.project.hems.simulator_service_testing.service;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.project.hems.simulator_service_testing.model.MeterSnapshot;
import com.project.hems.simulator_service_testing.web.exception.MeterStatusNotFoudException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MeterRedisService {

    private final RedisTemplate<String, MeterSnapshot> redisTemplate;

    // YOUR EXISTING PUBLISH METHOD
    public void publish(MeterSnapshot snapshot) {
        String key = "meter:" + snapshot.getUserId();
        redisTemplate.opsForValue().set(
                key,
                snapshot,
                Duration.ofSeconds(20) // Increased TTL slightly to be safe
        );
    }

    // NEW METHOD: WE NEED THIS FOR SIMULATION
    public MeterSnapshot getSnapshot(Long userId) {
        String key = "meter:" + userId;
        // Cast the Object back to MeterSnapshot
        MeterSnapshot snapshot = (MeterSnapshot) redisTemplate.opsForValue().get(key);
        if (snapshot == null) {
            throw new MeterStatusNotFoudException(
                    "unable to find the meter details for user id = " + userId);
        }
        return snapshot;
    }
}
