package com.nadoumi.communication.stream;

import com.nadoumi.communication.mapper.ConversationParticipantMapper;
import com.nadoumi.communication.mapper.LastSeenRow;
import com.nadoumi.communication.mapper.UserPresenceMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Who is connected right now, across every app instance. Each live SSE connection is a member of a per-user Redis
 * sorted set whose score is the moment it expires; the connection's heartbeat pushes that moment forward, so a
 * crashed instance's connections simply age out. A user is online while any member has not expired. When the last
 * connection closes the time is stored as the user's last-seen, and the people who share a conversation with
 * them are told.
 */
@Service
public class PresenceService {

    /** Long enough to ride out one missed heartbeat, short enough that a dead tab stops looking online quickly. */
    static final long CONNECTION_TTL_MILLIS = 60_000;
    private static final String KEY_PREFIX = "nadoumi:presence:";

    private final StringRedisTemplate redis;
    private final UserPresenceMapper lastSeen;
    private final ConversationParticipantMapper participants;
    private final RealtimePublisher realtime;

    public PresenceService(StringRedisTemplate communicationStringRedisTemplate, UserPresenceMapper lastSeen,
            ConversationParticipantMapper participants, RealtimePublisher realtime) {
        this.redis = communicationStringRedisTemplate;
        this.lastSeen = lastSeen;
        this.participants = participants;
        this.realtime = realtime;
    }

    /** A user's presence as peers see it. */
    public record Presence(boolean online, Instant lastSeenAt) {
    }

    /** Wire body of a {@code presence} event. */
    public record PresenceEvent(long userId, boolean online, Instant lastSeenAt) {
    }

    public void connected(long userId, String connectionId) {
        boolean wasOnline = isOnline(userId);
        touch(userId, connectionId);
        if (!wasOnline) {
            broadcast(userId, new Presence(true, null));
        }
    }

    /** Called from the connection heartbeat. */
    public void touch(long userId, String connectionId) {
        String key = key(userId);
        redis.opsForZSet().add(key, connectionId, System.currentTimeMillis() + CONNECTION_TTL_MILLIS);
        redis.expire(key, Duration.ofMillis(CONNECTION_TTL_MILLIS * 2));
    }

    public void disconnected(long userId, String connectionId) {
        redis.opsForZSet().remove(key(userId), connectionId);
        if (!isOnline(userId)) {
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            lastSeen.upsert(userId, now);
            broadcast(userId, new Presence(false, now.toInstant(ZoneOffset.UTC)));
        }
    }

    public boolean isOnline(long userId) {
        Long live = redis.opsForZSet().count(key(userId), System.currentTimeMillis(), Double.POSITIVE_INFINITY);
        return live != null && live > 0;
    }

    /** Presence for many users: one Redis call each and a single query for the last-seen times of those offline. */
    public Map<Long, Presence> presenceOf(Collection<Long> userIds) {
        Set<Long> ids = new HashSet<>(userIds);
        if (ids.isEmpty()) {
            return Map.of();
        }
        Set<Long> offline = new HashSet<>();
        Map<Long, Presence> out = new HashMap<>();
        for (Long id : ids) {
            if (isOnline(id)) {
                out.put(id, new Presence(true, null));
            }
            else {
                offline.add(id);
            }
        }
        if (!offline.isEmpty()) {
            Map<Long, Instant> seen = new HashMap<>();
            for (LastSeenRow row : lastSeen.listLastSeen(offline)) {
                seen.put(row.userId(), row.lastSeenAt().toInstant(ZoneOffset.UTC));
            }
            offline.forEach(id -> out.put(id, new Presence(false, seen.get(id))));
        }
        return out;
    }

    private void broadcast(long userId, Presence presence) {
        PresenceEvent event = new PresenceEvent(userId, presence.online(), presence.lastSeenAt());
        for (Long peer : participants.listPeerUserIds(userId)) {
            realtime.publish(peer, "presence", event);
        }
    }

    private static String key(long userId) {
        return KEY_PREFIX + userId;
    }
}
