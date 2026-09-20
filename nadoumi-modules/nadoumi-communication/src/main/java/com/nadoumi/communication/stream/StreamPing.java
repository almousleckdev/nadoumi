package com.nadoumi.communication.stream;

/**
 * The pub/sub wire payload: which user the ping is for, its kind, and the id the
 * client re-fetches by. Never the message/notification body itself.
 */
public record StreamPing(long userId, String type, long refId) {
}
