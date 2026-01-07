package com.project.hems.simulator_service_testing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.project.hems.simulator_service_testing.model.MeterSnapshot;

@Configuration
public class RedisConfig {

    @Bean
    LettuceConnectionFactory connectionFactory() {
        return new LettuceConnectionFactory();
    }

    @Bean
    public RedisTemplate<String, MeterSnapshot> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, MeterSnapshot> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use Jackson to convert our MeterSnapshot object to JSON
        Jackson2JsonRedisSerializer<MeterSnapshot> serializer = new Jackson2JsonRedisSerializer<>(MeterSnapshot.class);

        // Keys will be Strings (e.g., "SIMULATION_METERS")
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Values will be JSON
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        return template;
    }
}
