package com.nadoumi.communication.stream;

/**
 * Fixed Redis pub/sub channels every app instance publishes to and subscribes on
 * (docs/superpowers/specs/2026-09-20-messaging-domain-design.md §3). A small ping
 * payload only -- {@code {type, refId}} -- never the message body itself; clients
 * re-fetch the actual resource over the existing REST endpoints.
 */
public final class StreamChannels {

    private StreamChannels() {
    }

    /** IN_APP notification pings. Published by nadoumi-notification (a follow-up; not wired by this module). */
    public static final String NOTIFICATION = "nadoumi:stream:notification";

    /** New-message pings, published by this module's {@code MessagePublisher}. */
    public static final String CONVERSATION = "nadoumi:stream:conversation";
}
