package com.nadoumi.notification.outbox;

import com.nadoumi.common.outbox.OutboxWriter;
import com.nadoumi.notification.domain.OutboxEvent;
import com.nadoumi.notification.mapper.OutboxEventMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * The sole {@link OutboxWriter}. Inserts one PENDING {@code nad_outbox_event} row
 * on the caller's transaction ({@link Propagation#MANDATORY} — a producer that
 * forgot its {@code @Transactional} fails fast rather than emitting an event that
 * could outlive a rolled-back state change).
 */
public class OutboxWriterImpl implements OutboxWriter {

    private static final int MAX_PAYLOAD_CHARS = 2000;

    private final OutboxEventMapper mapper;

    public OutboxWriterImpl(OutboxEventMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public long write(String aggregateType, long aggregateId, String type, String payloadJson) {
        Assert.hasText(aggregateType, "aggregateType is required");
        Assert.hasText(type, "event type is required");
        String payload = payloadJson == null || payloadJson.isBlank() ? "{}" : payloadJson;
        Assert.isTrue(payload.length() <= MAX_PAYLOAD_CHARS,
                "outbox payload exceeds " + MAX_PAYLOAD_CHARS + " chars");

        OutboxEvent event = new OutboxEvent();
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setType(type);
        event.setPayloadJson(payload);
        mapper.insert(event);
        return event.getId();
    }
}
