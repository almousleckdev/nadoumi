package com.nadoumi.communication.mapper;

import java.time.LocalDateTime;

/** {@code lastSeenAt} is UTC. */
public record LastSeenRow(long userId, LocalDateTime lastSeenAt) {
}
