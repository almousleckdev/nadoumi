package com.nadoumi.communication.stream;

import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes a new-message ping to every app instance via Redis. A dropped Redis
 * connection silently stops realtime delivery -- the client's REST polling
 * fallback covers that, so a publish failure here is logged and swallowed, never
 * allowed to fail the message post itself.
 */
@Component
public class RealtimePublisher {

    private static final Logger log = LoggerFactory.getLogger(RealtimePublisher.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RealtimePublisher(StringRedisTemplate communicationStringRedisTemplate, ObjectMapper objectMapper) {
        this.redis = communicationStringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishConversationPing(long userId, long conversationId) {
        publish(StreamChannels.CONVERSATION, new StreamPing(userId, "conversation", conversationId));
    }

    private void publish(String channel, StreamPing ping) {
        try {
            redis.convertAndSend(channel, objectMapper.writeValueAsString(ping));
        }
        catch (Exception e) {
            log.warn("failed to publish a realtime ping on {} for user {} -- polling fallback covers it",
                    channel, ping.userId(), e);
        }
    }
}
