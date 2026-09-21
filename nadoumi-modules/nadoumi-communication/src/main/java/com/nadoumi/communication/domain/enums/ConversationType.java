package com.nadoumi.communication.domain.enums;

/**
 * Discriminator left open for the Support/Ticketing domain to build a ticket
 * record that owns a {@code SUPPORT}-typed conversation instead of reinventing
 * threading (docs/superpowers/specs/2026-09-20-messaging-domain-design.md §7).
 * This module does not implement or assume anything about that ticket table.
 */
public enum ConversationType {
    GENERAL,
    SUPPORT
}
