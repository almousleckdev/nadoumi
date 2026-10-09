package com.nadoumi.communication.stream;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Opens a user's chat stream: registers it, greets the client, and marks what was sent while they were away as delivered. */
@Service
public class ChatStreamService {

    private static final long TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(30);

    private final SseConnectionRegistry registry;
    private final DeliveryTracker deliveries;

    public ChatStreamService(SseConnectionRegistry registry, DeliveryTracker deliveries) {
        this.registry = registry;
        this.deliveries = deliveries;
    }

    public SseEmitter open(long userId) {
        SseEmitter emitter = registry.register(userId, TIMEOUT_MILLIS);
        try {
            emitter.send(SseEmitter.event().name("ready").data("{\"serverTime\":\"" + Instant.now() + "\"}"));
        }
        catch (IOException e) {
            emitter.completeWithError(e);
            return emitter;
        }
        deliveries.caughtUp(userId);
        return emitter;
    }
}
