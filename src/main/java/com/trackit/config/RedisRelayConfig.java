package com.trackit.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.charset.StandardCharsets;

/**
 * Horizontal scaling: updates are published to a Redis channel instead of straight to WebSocket.
 * Every instance subscribes and forwards to the browsers connected to it.
 */
@Configuration
public class RedisRelayConfig {
    public static final String CHANNEL = "tracking";
    private static final Logger log = LoggerFactory.getLogger(RedisRelayConfig.class);

    @Bean
    RedisMessageListenerContainer relayContainer(RedisConnectionFactory cf, SimpMessagingTemplate ws, ObjectMapper om) {
        RedisMessageListenerContainer c = new RedisMessageListenerContainer();
        c.setConnectionFactory(cf);
        c.addMessageListener((message, pattern) -> {
            try {
                JsonNode n = om.readTree(new String(message.getBody(), StandardCharsets.UTF_8));
                ws.convertAndSend(n.get("destination").asText(), n.get("payload").toString());
            } catch (Exception e) {
                log.warn("Bad tracking message", e);
            }
        }, new ChannelTopic(CHANNEL));
        return c;
    }
}
