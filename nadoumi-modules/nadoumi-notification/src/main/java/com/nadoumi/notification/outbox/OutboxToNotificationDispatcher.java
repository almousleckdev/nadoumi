package com.nadoumi.notification.outbox;

import com.alibaba.fastjson2.JSON;
import com.nadoumi.common.notification.NotificationChannelKind;
import com.nadoumi.common.outbox.OutboxEventTypes;
import com.nadoumi.notification.domain.NotificationScope;
import com.nadoumi.notification.domain.NotificationType;
import com.nadoumi.notification.domain.OutboxEvent;
import com.nadoumi.notification.mapper.NotificationAudienceMapper;
import com.nadoumi.notification.render.NotificationRenderer;
import com.nadoumi.notification.service.NotificationRequest;
import com.nadoumi.notification.service.NotificationService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Turns a drained {@code nad_outbox_event} into notifications. Idempotent: each
 * created row carries {@code source_ref = outbox:<eventId>:<userId>}, so the
 * poller's at-least-once redelivery is a no-op the second time.
 *
 * <p>Strict scope isolation: {@link NotificationScope#TARGETED} notifications
 * MUST have explicit recipient(s) and NEVER broadcast. Only {@link NotificationScope#GLOBAL}
 * public catalog announcements reach staff and students, and {@link NotificationScope#STUDENTS}
 * announcements (news) reach active students only.</p>
 */
public class OutboxToNotificationDispatcher implements OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OutboxToNotificationDispatcher.class);

    private static final String AUDIENCE_PERMISSION = "nad:notification:list";

    private final NotificationService notificationService;
    private final NotificationRenderer renderer;
    private final NotificationAudienceMapper audienceMapper;
    private final WelcomeContentComposer welcomeComposer;

    public OutboxToNotificationDispatcher(NotificationService notificationService, NotificationRenderer renderer,
            NotificationAudienceMapper audienceMapper, WelcomeContentComposer welcomeComposer) {
        this.notificationService = notificationService;
        this.renderer = renderer;
        this.audienceMapper = audienceMapper;
        this.welcomeComposer = welcomeComposer;
    }

    @Override
    public void dispatch(OutboxEvent event) {
        if (OutboxEventTypes.STUDENT_REGISTERED.equals(event.getType())) {
            welcomeComposer.handle(event, parseContext(event));
            return;
        }

        NotificationType type = mapType(event.getType());
        if (type == null) {
            log.warn("outbox event id={} type={} has no notification mapping — skipped",
                    event.getId(), event.getType());
            return;
        }

        Map<String, Object> context = parseContext(event);
        List<Long> recipients = recipientsFromPayload(context);
        if (recipients == null || recipients.isEmpty()) {
            if (type.scope() == NotificationScope.TARGETED) {
                // If it's a contact inquiry or ticket opened without explicit assignees, resolve authorized staff
                if (type == NotificationType.CONTACT_INQUIRY_RECEIVED || type == NotificationType.TICKET_OPENED) {
                    recipients = new ArrayList<>(audienceMapper.findStaffUserIdsWithPermission(audiencePermission(context)));
                } else {
                    log.warn("outbox event id={} type={} is TARGETED but has no recipient user ID — skipped to prevent leakage",
                            event.getId(), type);
                    return;
                }
            } else if (type.scope() == NotificationScope.STUDENTS) {
                recipients = audienceMapper.findActiveStudentUserIds();
            } else {
                // GLOBAL catalog announcement: staff holding catalog permission + active registered students
                LinkedHashSet<Long> set =
                        new LinkedHashSet<>(audienceMapper.findStaffUserIdsWithPermission(audiencePermission(context)));
                set.addAll(audienceMapper.findActiveStudentUserIds());
                recipients = new ArrayList<>(set);
            }
        }
        if (recipients.isEmpty()) {
            log.warn("outbox event id={} type={} resolved to empty recipients",
                    event.getId(), type);
            return;
        }

        String body = renderer.render(type, NotificationChannelKind.IN_APP,
                NotificationRenderer.DEFAULT_LOCALE, context).body();

        Long applicationId = longFromPayload(context, "applicationId");
        Long conversationId = longFromPayload(context, "conversationId");
        Long messageId = longFromPayload(context, "messageId");

        List<NotificationRequest> batch = new ArrayList<>(recipients.size());
        for (Long userId : recipients) {
            batch.add(NotificationRequest.fromEvent(userId, type, type.defaultTitle(), body,
                    "outbox:" + event.getId() + ":" + userId, applicationId, conversationId, messageId,
                    event.getPayloadJson()));
        }
        notificationService.createBatch(batch);
    }

    private static NotificationType mapType(String eventType) {
        return switch (eventType) {
            case OutboxEventTypes.CONTACT_INQUIRY_RECEIVED -> NotificationType.CONTACT_INQUIRY_RECEIVED;
            case OutboxEventTypes.SCHOLARSHIP_PUBLISHED -> NotificationType.SCHOLARSHIP_PUBLISHED;
            case OutboxEventTypes.SCHOLARSHIP_DEADLINE_REMINDER -> NotificationType.SCHOLARSHIP_DEADLINE_REMINDER;
            case OutboxEventTypes.UNIVERSITY_PUBLISHED -> NotificationType.UNIVERSITY_PUBLISHED;
            case OutboxEventTypes.PROGRAM_PUBLISHED -> NotificationType.PROGRAM_PUBLISHED;
            case OutboxEventTypes.ARTICLE_PUBLISHED -> NotificationType.ARTICLE_PUBLISHED;
            case OutboxEventTypes.TASK_PROGRESS_CHANGED -> NotificationType.TASK_PROGRESS;
            case OutboxEventTypes.APPLICATION_SUBMITTED -> NotificationType.APPLICATION_SUBMITTED;
            case OutboxEventTypes.APPLICATION_STATUS_CHANGED -> NotificationType.APPLICATION_STATUS_CHANGED;
            case OutboxEventTypes.MESSAGE_POSTED -> NotificationType.MESSAGE_POSTED;
            case OutboxEventTypes.TICKET_OPENED -> NotificationType.TICKET_OPENED;
            case OutboxEventTypes.TICKET_ASSIGNED -> NotificationType.TICKET_ASSIGNED;
            case OutboxEventTypes.TICKET_STATUS_CHANGED -> NotificationType.TICKET_STATUS_CHANGED;
            default -> null;
        };
    }

    /**
     * A soft-reference id (e.g. {@code conversationId}) from the outbox payload, or
     * {@code null} when the event type doesn't carry one — same soft-reference
     * treatment as {@code nad_notification.application_id}/{@code conversation_id}/
     * {@code message_id} themselves (no FK, populated only when present).
     */
    private static Long longFromPayload(Map<String, Object> context, String key) {
        Object raw = context.get(key);
        return raw == null ? null : Long.parseLong(String.valueOf(raw));
    }

    /**
     * Staff audience permission: the payload's {@code audiencePermission} when the producer
     * names one (support tickets target {@code nad:support:ticket:view}), else the default
     * {@link #AUDIENCE_PERMISSION}.
     */
    private static String audiencePermission(Map<String, Object> context) {
        Object raw = context.get("audiencePermission");
        return raw == null || String.valueOf(raw).isBlank() ? AUDIENCE_PERMISSION : String.valueOf(raw);
    }

    /**
     * Explicit recipient list from a {@code recipientUserIds} array or single {@code recipientUserId} /
     * {@code userId} in the payload; {@code null} means "resolve by permission/scope instead".
     */
    private static List<Long> recipientsFromPayload(Map<String, Object> context) {
        Object raw = context.get("recipientUserIds");
        if (raw instanceof List<?> list) {
            List<Long> result = list.stream()
                    .filter(Objects::nonNull)
                    .map(v -> Long.parseLong(String.valueOf(v)))
                    .distinct()
                    .toList();
            if (!result.isEmpty()) {
                return result;
            }
        }
        for (String key : List.of("recipientUserId", "userId", "studentUserId")) {
            Object single = context.get(key);
            if (single != null) {
                try {
                    return List.of(Long.parseLong(String.valueOf(single)));
                } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    private static Map<String, Object> parseContext(OutboxEvent event) {
        String json = event.getPayloadJson();
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        return JSON.parseObject(json);
    }
}
