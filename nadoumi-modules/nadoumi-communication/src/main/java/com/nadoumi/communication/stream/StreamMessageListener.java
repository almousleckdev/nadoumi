package com.nadoumi.communication.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.Message;
import org.springframework.stereotype.Component;

/**
 * Relays a {@link StreamPing} received on either {@link StreamChannels} channel to
 * this instance's own locally-connected clients via {@link SseConnectionRegistry}.
 * One listener, both channels -- the channel name only tells us who published, the
 * ping payload's own {@code type} is what the client keys off.
 */
@Component
public class StreamMessageListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(StreamMessageListener.class);

    private final SseConnectionRegistry registry;
    private final ObjectMapper objectMapper;

    public StreamMessageListener(SseConnectionRegistry registry, ObjectMapper objectMapper) {
        this.registry = registry;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            StreamPing ping = objectMapper.readValue(message.getBody(), StreamPing.class);
            registry.push(ping);
        }
        catch (Exception e) {
            log.warn("dropped a malformed stream ping on channel {}", new String(message.getChannel()), e);
        }
    }
}
