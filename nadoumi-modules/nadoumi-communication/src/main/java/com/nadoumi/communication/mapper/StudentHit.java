package com.nadoumi.communication.mapper;

/** A student matched by a staff search; {@code applicationId} is set when the match came through an application id. */
public record StudentHit(long userId, Long applicationId) {
}
