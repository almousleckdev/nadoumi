package com.nadoumi.communication.stream;

import com.nadoumi.communication.web.response.MessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Relays a {@link StreamEvent} received from Redis to this instance's own connected clients. When the event is a
 * new message and the recipient's connection accepted it, the message counts as delivered.
 */
@Component
public class StreamMessageListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(StreamMessageListener.class);

    static final String MESSAGE_EVENT = "message";

    private final SseConnectionRegistry registry;
    private final DeliveryTracker deliveries;
    private final ObjectMapper objectMapper;

    public StreamMessageListener(SseConnectionRegistry registry, DeliveryTracker deliveries, ObjectMapper objectMapper) {
        this.registry = registry;
        this.deliveries = deliveries;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            StreamEvent event = objectMapper.readValue(message.getBody(), StreamEvent.class);
            boolean pushed = registry.push(event);
            if (pushed && MESSAGE_EVENT.equals(event.type())) {
                MessageResponse posted = objectMapper.readValue(event.data(), MessageResponse.class);
                deliveries.delivered(posted.conversationId(), event.userId(), posted.id(), posted.senderUserId());
            }
        }
        catch (Exception e) {
            log.warn("dropped a malformed stream event on channel {}", new String(message.getChannel()), e);
        }
    }
}
