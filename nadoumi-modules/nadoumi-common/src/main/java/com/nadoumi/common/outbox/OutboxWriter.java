package com.nadoumi.common.outbox;

/**
 * Writes a domain event to the transactional outbox. Producers call this from
 * inside their own {@code @Transactional} service method, so the event row commits
 * atomically with the state change that produced it (see
 * {@code docs/DOMAIN_EVENTS.md}). A poller drains the outbox asynchronously and
 * hands each event to the notification pipeline.
 *
 * <p>{@code payloadJson} must carry IDs and safe scalars only — never PII, never
 * confidential fields ({@code docs/SECURITY.md} §6). Renderers resolve details
 * later through authorized queries.</p>
 *
 * <p>The single implementation lives in {@code nadoumi-notification}; domain
 * modules depend on this interface only.</p>
 */
public interface OutboxWriter {

    /**
     * Append one event to the outbox.
     *
     * @param aggregateType short lowercase noun for the source aggregate,
     *                      e.g. {@code "scholarship"} or {@code "contact_inquiry"}
     * @param aggregateId   the source aggregate's primary key
     * @param type          the domain event type ({@link OutboxEventTypes})
     * @param payloadJson   a compact JSON object of safe scalars, or {@code "{}"}
     * @return the generated {@code nad_outbox_event} id
     * @throws IllegalStateException when called with no active transaction
     */
    long write(String aggregateType, long aggregateId, String type, String payloadJson);
}
