package com.nadoumi.communication.stream;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * The {@link SseEmitter}s held by this app instance, keyed by user id. Multi-instance correctness comes from every
 * instance publishing to Redis and relaying only to its own emitters, so this registry never needs to know about
 * other instances. It also keeps each connection alive (a heartbeat, 20s by default, which proxies need and
 * which refreshes presence, and which is also how a closed browser tab is noticed) and reports opens and closes to {@link PresenceService}.
 */
@Component
public class SseConnectionRegistry {

    private static final Logger log = LoggerFactory.getLogger(SseConnectionRegistry.class);

    private record Connection(String id, SseEmitter emitter) {
    }

    private final Map<Long, List<Connection>> byUser = new ConcurrentHashMap<>();
    private final PresenceService presence;
    private final ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "chat-sse-heartbeat");
        t.setDaemon(true);
        return t;
    });

    public SseConnectionRegistry(PresenceService presence,
            @Value("${nadoumi.chat.heartbeat-seconds:20}") long heartbeatSeconds) {
        this.presence = presence;
        heartbeat.scheduleWithFixedDelay(this::beat, heartbeatSeconds, heartbeatSeconds, TimeUnit.SECONDS);
    }

    public SseEmitter register(long userId, long timeoutMillis) {
        SseEmitter emitter = new SseEmitter(timeoutMillis);
        Connection connection = new Connection(UUID.randomUUID().toString(), emitter);
        byUser.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(connection);
        emitter.onCompletion(() -> remove(userId, connection));
        emitter.onTimeout(() -> remove(userId, connection));
        emitter.onError(e -> remove(userId, connection));
        presence.connected(userId, connection.id());
        return emitter;
    }

    /** True when at least one connection for {@code userId} is held by this instance. */
    public boolean hasLocalConnection(long userId) {
        List<Connection> connections = byUser.get(userId);
        return connections != null && !connections.isEmpty();
    }

    /** Pushes {@code event} to every local connection of its user; true when at least one accepted it. */
    public boolean push(StreamEvent event) {
        List<Connection> connections = byUser.get(event.userId());
        if (connections == null || connections.isEmpty()) {
            return false;
        }
        boolean delivered = false;
        for (Connection connection : List.copyOf(connections)) {
            if (send(connection, SseEmitter.event().name(event.type()).data(event.data()))) {
                delivered = true;
            }
            else {
                remove(event.userId(), connection);
            }
        }
        return delivered;
    }

    private void beat() {
        try {
            byUser.forEach((userId, connections) -> {
                for (Connection connection : List.copyOf(connections)) {
                    if (send(connection, SseEmitter.event().name("heartbeat").data("{}"))) {
                        presence.touch(userId, connection.id());
                    }
                    else {
                        remove(userId, connection);
                    }
                }
            });
        }
        catch (RuntimeException e) {
            // one bad round (e.g. Redis briefly down) must not cancel the schedule
            log.warn("chat heartbeat round failed", e);
        }
    }

    private boolean send(Connection connection, SseEmitter.SseEventBuilder event) {
        try {
            connection.emitter().send(event);
            return true;
        }
        catch (IOException | IllegalStateException e) {
            log.debug("dropping a dead SSE connection {}", connection.id());
            return false;
        }
    }

    private void remove(long userId, Connection connection) {
        List<Connection> connections = byUser.get(userId);
        if (connections == null || !connections.remove(connection)) {
            return;
        }
        if (connections.isEmpty()) {
            byUser.remove(userId, connections);
        }
        presence.disconnected(userId, connection.id());
    }

    @PreDestroy
    void shutdown() {
        heartbeat.shutdownNow();
    }
}
