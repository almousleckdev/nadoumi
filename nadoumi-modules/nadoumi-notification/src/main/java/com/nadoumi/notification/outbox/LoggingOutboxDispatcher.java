package com.nadoumi.notification.outbox;

import com.nadoumi.notification.domain.OutboxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link OutboxDispatcher} until the notification pipeline lands: records
 * that an event was drained and acknowledges it. Logs the type and aggregate
 * reference only — never the payload body ({@code docs/SECURITY.md} §6).
 */
public class LoggingOutboxDispatcher implements OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(LoggingOutboxDispatcher.class);

    @Override
    public void dispatch(OutboxEvent event) {
        log.info("outbox event drained (no consumer yet): id={} type={} aggregate={}/{}",
                event.getId(), event.getType(), event.getAggregateType(), event.getAggregateId());
    }
}
