package com.trackit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackit.config.RedisRelayConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Publishes {destination, payload} to Redis; RedisRelayConfig turns it into STOMP messages on every instance. */
@Component
@RequiredArgsConstructor
public class TrackingPublisher {
    private final StringRedisTemplate redis;
    private final ObjectMapper om;

    public void publish(String destination, Object payload) {
        try {
            redis.convertAndSend(RedisRelayConfig.CHANNEL,
                    om.writeValueAsString(Map.of("destination", destination, "payload", payload)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
