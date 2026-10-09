package com.nadoumi.communication.stream;

import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Publishes a chat event to every app instance via Redis. A failed publish is logged and swallowed: the message
 * is already stored, and the client resynchronises over REST when its stream reconnects.
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

    /** Sends now. Use {@link #afterCommit} when the event describes data written in the current transaction. */
    public void publish(long userId, String type, Object payload) {
        try {
            String data = objectMapper.writeValueAsString(payload);
            redis.convertAndSend(StreamChannels.CONVERSATION,
                    objectMapper.writeValueAsString(new StreamEvent(userId, type, data)));
        }
        catch (Exception e) {
            log.warn("failed to publish a {} event for user {}; the client will resync on reconnect", type, userId, e);
        }
    }

    /** Sends once the surrounding transaction commits, so a client never hears about a row that rolled back. */
    public void afterCommit(long userId, String type, Object payload) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publish(userId, type, payload);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publish(userId, type, payload);
            }
        });
    }
}
