package com.nadoumi.notification.outbox;

import com.alibaba.fastjson2.JSON;
import com.nadoumi.common.notification.NotificationChannelKind;
import com.nadoumi.common.outbox.OutboxEventTypes;
import com.nadoumi.notification.domain.NotificationType;
import com.nadoumi.notification.domain.OutboxEvent;
import com.nadoumi.notification.mapper.NotificationAudienceMapper;
import com.nadoumi.notification.render.NotificationRenderer;
import com.nadoumi.notification.service.NotificationRequest;
import com.nadoumi.notification.service.NotificationService;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Turns a drained {@code nad_outbox_event} into notifications. Idempotent: each
 * created row carries {@code source_ref = outbox:<eventId>:<userId>}, so the
 * poller's at-least-once redelivery is a no-op the second time.
 *
 * <p>v1 audience for both wired events is <b>staff holding
 * {@code nad:notification:list}</b> — for {@code ContactInquiryReceived} that is
 * the support desk; for {@code ScholarshipPublished} it is an operational audit
 * note (student targeting by saved-search / opt-in is a later step, see
 * {@code docs/DOMAIN_EVENTS.md}).</p>
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
        if (recipients == null) {
            java.util.LinkedHashSet<Long> set =
                    new java.util.LinkedHashSet<>(audienceMapper.findStaffUserIdsWithPermission(AUDIENCE_PERMISSION));
            if (isPublicCatalogAnnouncement(type)) {
                // registered students hear about every new university / programme / scholarship
                set.addAll(audienceMapper.findActiveStudentUserIds());
            }
            recipients = new java.util.ArrayList<>(set);
        }
        if (recipients.isEmpty()) {
            log.warn("outbox event id={} type={} has no staff recipients ({})",
                    event.getId(), type, AUDIENCE_PERMISSION);
            return;
        }

        String body = renderer.render(type, NotificationChannelKind.IN_APP,
                NotificationRenderer.DEFAULT_LOCALE, context).body();

        List<NotificationRequest> batch = new java.util.ArrayList<>(recipients.size());
        for (Long userId : recipients) {
            batch.add(NotificationRequest.fromEvent(userId, type, type.defaultTitle(), body,
                    "outbox:" + event.getId() + ":" + userId, event.getPayloadJson()));
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
            case OutboxEventTypes.TASK_PROGRESS_CHANGED -> NotificationType.TASK_PROGRESS;
            case OutboxEventTypes.APPLICATION_SUBMITTED -> NotificationType.APPLICATION_SUBMITTED;
            case OutboxEventTypes.APPLICATION_STATUS_CHANGED -> NotificationType.APPLICATION_STATUS_CHANGED;
            default -> null;
        };
    }

    /** Types every registered student is told about, not just the staff audit audience. */
    private static boolean isPublicCatalogAnnouncement(NotificationType type) {
        return type == NotificationType.SCHOLARSHIP_PUBLISHED
                || type == NotificationType.SCHOLARSHIP_DEADLINE_REMINDER
                || type == NotificationType.UNIVERSITY_PUBLISHED
                || type == NotificationType.PROGRAM_PUBLISHED;
    }

    /**
     * Explicit recipient list from a {@code recipientUserIds} array in the payload
     * (used by task events); {@code null} means "resolve by permission instead".
     */
    private static List<Long> recipientsFromPayload(Map<String, Object> context) {
        Object raw = context.get("recipientUserIds");
        if (!(raw instanceof List<?> list)) {
            return null;
        }
        return list.stream()
                .filter(java.util.Objects::nonNull)
                .map(v -> Long.parseLong(String.valueOf(v)))
                .distinct()
                .toList();
    }

    private static Map<String, Object> parseContext(OutboxEvent event) {
        String json = event.getPayloadJson();
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        return JSON.parseObject(json);
    }
}
