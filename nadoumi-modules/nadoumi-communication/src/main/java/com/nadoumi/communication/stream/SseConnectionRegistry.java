package com.nadoumi.communication.stream;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Locally-connected {@link SseEmitter}s for this app instance, keyed by user id.
 * Multi-instance correctness comes from every instance publishing to Redis and
 * every instance's listener relaying only to its own locally-held emitters
 * (docs/superpowers/specs/2026-09-20-messaging-domain-design.md §3) -- this
 * registry never needs to know about other instances.
 */
@Component
public class SseConnectionRegistry {

    private static final Logger log = LoggerFactory.getLogger(SseConnectionRegistry.class);

    private final Map<Long, List<SseEmitter>> byUser = new ConcurrentHashMap<>();

    public SseEmitter register(long userId, long timeoutMillis) {
        SseEmitter emitter = new SseEmitter(timeoutMillis);
        List<SseEmitter> emitters = byUser.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));
        return emitter;
    }

    public void remove(long userId, SseEmitter emitter) {
        List<SseEmitter> emitters = byUser.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                byUser.remove(userId, emitters);
            }
        }
    }

    /** True when at least one connection for {@code userId} is held by this instance. */
    public boolean hasLocalConnection(long userId) {
        List<SseEmitter> emitters = byUser.get(userId);
        return emitters != null && !emitters.isEmpty();
    }

    /** Push {@code ping} to every locally-connected emitter for its {@code userId}. Dead emitters are dropped. */
    public void push(StreamPing ping) {
        List<SseEmitter> emitters = byUser.get(ping.userId());
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : List.copyOf(emitters)) {
            try {
                emitter.send(SseEmitter.event().name(ping.type()).data(Map.of("refId", ping.refId())));
            }
            catch (IOException e) {
                log.debug("dropping a dead SSE connection for user {}", ping.userId());
                remove(ping.userId(), emitter);
            }
        }
    }
}
