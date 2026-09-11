package com.nadoumi.notification.dispatch;

import com.alibaba.fastjson2.JSON;
import com.nadoumi.common.notification.NotificationChannel;
import com.nadoumi.common.notification.NotificationChannelKind;
import com.nadoumi.common.notification.NotificationSendResult;
import com.nadoumi.common.notification.NotificationSendStatus;
import com.nadoumi.notification.domain.DeliveryStatus;
import com.nadoumi.notification.domain.Notification;
import com.nadoumi.notification.domain.NotificationDelivery;
import com.nadoumi.notification.domain.NotificationType;
import com.nadoumi.notification.mapper.NotificationDeliveryMapper;
import com.nadoumi.notification.mapper.NotificationMapper;
import com.nadoumi.notification.mapper.NotificationRecipientMapper;
import com.nadoumi.notification.render.NotificationRenderer;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drains PENDING {@code nad_notification_delivery} rows through their channel.
 * Renders the per-channel template against the notification's {@code data_json},
 * resolves the recipient address, calls the channel, and moves the row to
 * {@code SENT} — or, on failure, {@code retry_count}s it with backoff and parks it
 * {@code FAILED} after {@link #MAX_ATTEMPTS}. {@code IN_APP} rows are created
 * already-{@code SENT}, so they never reach here.
 *
 * <p>Invoked by {@code NotificationDispatchJob} (Quartz {@code sys_job}, V34).</p>
 */
public class NotificationDeliveryDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDeliveryDispatcher.class);

    static final int BATCH_SIZE = 100;
    static final int MAX_ATTEMPTS = 6;
    private static final long BACKOFF_STEP_SECONDS = 60L;
    private static final long BACKOFF_CAP_SECONDS = 3600L;
    private static final int LAST_ERROR_MAX_CHARS = 500;

    private final NotificationDeliveryMapper deliveryMapper;
    private final NotificationMapper notificationMapper;
    private final NotificationRecipientMapper recipientMapper;
    private final NotificationRenderer renderer;
    private final Map<NotificationChannelKind, NotificationChannel> channels;

    public NotificationDeliveryDispatcher(NotificationDeliveryMapper deliveryMapper,
            NotificationMapper notificationMapper, NotificationRecipientMapper recipientMapper,
            NotificationRenderer renderer, List<NotificationChannel> channelBeans) {
        this.deliveryMapper = deliveryMapper;
        this.notificationMapper = notificationMapper;
        this.recipientMapper = recipientMapper;
        this.renderer = renderer;
        this.channels = channelBeans.stream()
                .collect(Collectors.toMap(NotificationChannel::kind, Function.identity()));
    }

    private enum Outcome { SENT, RETRYING, DEAD }

    /** Quartz entry point. */
    public void run() {
        List<NotificationDelivery> batch = deliveryMapper.findDispatchable(BATCH_SIZE);
        if (batch.isEmpty()) {
            return;
        }
        int sent = 0;
        int retrying = 0;
        int dead = 0;
        for (NotificationDelivery delivery : batch) {
            Outcome outcome = attempt(delivery);
            switch (outcome) {
                case SENT -> sent++;
                case RETRYING -> retrying++;
                case DEAD -> dead++;
            }
        }
        log.info("notification dispatch: scanned={} sent={} retrying={} dead={}",
                batch.size(), sent, retrying, dead);
    }

    private Outcome attempt(NotificationDelivery delivery) {
        try {
            NotificationChannelKind kind = NotificationChannelKind.valueOf(delivery.getChannel());
            NotificationChannel channel = channels.get(kind);
            if (channel == null) {
                return fail(delivery, "no channel implementation for " + kind, true);
            }
            Notification notification = notificationMapper.findById(delivery.getNotificationId());
            if (notification == null) {
                return fail(delivery, "notification " + delivery.getNotificationId() + " is gone", true);
            }
            NotificationType type = NotificationType.valueOf(notification.getType());

            String recipient = resolveRecipient(kind, notification);
            if (recipient == null || recipient.isBlank()) {
                return fail(delivery, "no " + kind + " address for user " + notification.getRecipientUserId(), true);
            }

            NotificationRenderer.Rendered rendered =
                    renderer.render(type, kind, NotificationRenderer.DEFAULT_LOCALE, context(notification));
            NotificationSendResult result = channel.send(recipient, rendered.subject(), rendered.body(), type.name());

            if (result.status() == NotificationSendStatus.SENT) {
                deliveryMapper.markSent(delivery.getId(), providerFor(kind),
                        result.providerMessageId(), LocalDateTime.now());
                return Outcome.SENT;
            }
            return fail(delivery, result.error(), false);
        } catch (RuntimeException e) {
            return fail(delivery, e.toString(), false);
        }
    }

    private String resolveRecipient(NotificationChannelKind kind, Notification notification) {
        return kind == NotificationChannelKind.EMAIL
                ? recipientMapper.findEmail(notification.getRecipientUserId())
                : String.valueOf(notification.getRecipientUserId());
    }

    private static String providerFor(NotificationChannelKind kind) {
        return kind == NotificationChannelKind.EMAIL
                ? com.nadoumi.notification.channel.EmailNotificationChannel.PROVIDER
                : kind.name().toLowerCase();
    }

    private Map<String, Object> context(Notification notification) {
        Map<String, Object> ctx = new HashMap<>();
        String json = notification.getDataJson();
        if (json != null && !json.isBlank()) {
            try {
                ctx.putAll(JSON.parseObject(json));
            } catch (RuntimeException e) {
                throw new IllegalStateException("notification " + notification.getId()
                        + " has malformed data_json", e);
            }
        }
        return ctx;
    }

    private Outcome fail(NotificationDelivery delivery, String error, boolean permanent) {
        int attempts = delivery.getAttempts() + 1;
        boolean dead = permanent || attempts >= MAX_ATTEMPTS;
        String status = dead ? DeliveryStatus.FAILED.name() : DeliveryStatus.PENDING.name();
        deliveryMapper.recordFailure(delivery.getId(), status, truncate(error), nextAttempt(attempts));
        if (dead) {
            log.error("notification delivery id={} channel={} parked FAILED: {}",
                    delivery.getId(), delivery.getChannel(), error);
            return Outcome.DEAD;
        }
        log.warn("notification delivery id={} channel={} attempt {} failed, will retry: {}",
                delivery.getId(), delivery.getChannel(), attempts, error);
        return Outcome.RETRYING;
    }

    private static LocalDateTime nextAttempt(int attempts) {
        long delay = Math.min(attempts * BACKOFF_STEP_SECONDS, BACKOFF_CAP_SECONDS);
        return LocalDateTime.now().plusSeconds(delay);
    }

    private static String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() <= LAST_ERROR_MAX_CHARS ? s : s.substring(0, LAST_ERROR_MAX_CHARS);
    }
}
