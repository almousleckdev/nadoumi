/**
 * Notifications. Owns the transactional outbox ({@code nad_outbox_event}) and its
 * poller, the {@code nad_notification*} model, per-locale templates, and channel
 * dispatch. Producers depend on {@link com.nadoumi.common.outbox.OutboxWriter};
 * nothing outside this module touches its mappers. The SSE stream endpoints and
 * Redis pub/sub infrastructure live in {@code nadoumi-communication}
 * ({@code com.nadoumi.communication.stream}) -- this module's IN_APP notifications
 * are one of the two ping types relayed over it (a follow-up wires the publish
 * side; see that package's Javadoc). See {@code docs/DOMAIN_EVENTS.md} and
 * {@code docs/COMMUNICATION_AND_NOTIFICATIONS.md}.
 */
package com.nadoumi.notification;
