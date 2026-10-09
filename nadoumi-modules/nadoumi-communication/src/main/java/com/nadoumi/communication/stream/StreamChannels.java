package com.nadoumi.communication.stream;

/**
 * Fixed Redis pub/sub channel every app instance publishes to and subscribes on
 * (docs/superpowers/specs/2026-09-20-messaging-domain-design.md §3). Each instance relays an event to its own
 * locally-connected clients only, which is what makes delivery correct without sticky sessions.
 */
public final class StreamChannels {

    private StreamChannels() {
    }

    /** All chat events: new messages, receipts and presence changes. */
    public static final String CONVERSATION = "nadoumi:stream:conversation";
}
