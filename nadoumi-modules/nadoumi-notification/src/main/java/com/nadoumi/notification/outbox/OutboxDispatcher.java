package com.nadoumi.notification.outbox;

import com.nadoumi.notification.domain.OutboxEvent;

/**
 * Consumes one event drained from the outbox and turns it into notifications.
 *
 * <p>Implementations must be idempotent — the poller delivers at least once, and
 * an event can be re-handed after a mid-dispatch failure. A thrown exception is
 * recorded as a failed attempt and retried with backoff; returning normally marks
 * the event {@code DONE}.</p>
 *
 * <p>Slice 1 ships {@link LoggingOutboxDispatcher}; the real mapping to
 * {@code NotificationService} replaces it in a later slice.</p>
 */
public interface OutboxDispatcher {

    void dispatch(OutboxEvent event);
}
