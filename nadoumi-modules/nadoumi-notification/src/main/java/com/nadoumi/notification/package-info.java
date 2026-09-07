/**
 * Notifications &amp; realtime transport. Owns the transactional outbox
 * ({@code nad_outbox_event}) and its poller, the {@code nad_notification*} model,
 * per-locale templates, channel dispatch, and the SSE stream. Producers depend on
 * {@link com.nadoumi.common.outbox.OutboxWriter}; nothing outside this module
 * touches its mappers. See {@code docs/DOMAIN_EVENTS.md} and
 * {@code docs/COMMUNICATION_AND_NOTIFICATIONS.md}.
 */
package com.nadoumi.notification;
