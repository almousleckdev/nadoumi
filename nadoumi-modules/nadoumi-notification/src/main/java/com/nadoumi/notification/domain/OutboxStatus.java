package com.nadoumi.notification.domain;

/**
 * Lifecycle of a {@code nad_outbox_event} row.
 *
 * <p>v1 uses only {@link #PENDING}, {@link #DONE} and {@link #FAILED}: RuoYi's
 * clustered Quartz store fires the poller on one instance at a time, so there is
 * no window where a row must be marked {@link #PROCESSING} to fence a second
 * worker. The value is kept for the envelope contract in
 * {@code docs/DOMAIN_EVENTS.md} and for a future multi-worker drainer.</p>
 */
public enum OutboxStatus {
    PENDING,
    PROCESSING,
    DONE,
    FAILED
}
