package com.nadoumi.communication.web.response;

/**
 * How a chat participant appears to the other side: a first name or username and a profile image, nothing
 * else (no surname, email, phone or id document). {@code avatarUrl} is null when there is no public photo, and {@code lastSeenAt} when the person has never connected or is online now.
 */
public record ChatPerson(long userId, String name, String avatarUrl, boolean online, java.time.Instant lastSeenAt) {
}
