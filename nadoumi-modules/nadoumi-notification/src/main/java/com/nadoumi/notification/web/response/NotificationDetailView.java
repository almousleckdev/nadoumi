package com.nadoumi.notification.web.response;

import com.nadoumi.notification.domain.Notification;
import com.nadoumi.notification.domain.NotificationDelivery;
import java.time.LocalDateTime;
import java.util.List;

/** Staff view of a notification and its per-channel delivery attempts. */
public record NotificationDetailView(
        long id,
        long recipientUserId,
        String type,
        String title,
        String body,
        String dataJson,
        LocalDateTime createdAt,
        LocalDateTime readAt,
        List<Delivery> deliveries) {

    public record Delivery(
            long id,
            String channel,
            String provider,
            String providerMessageId,
            String status,
            int attempts,
            String lastError,
            LocalDateTime sentAt) {

        static Delivery from(NotificationDelivery d) {
            return new Delivery(d.getId(), d.getChannel(), d.getProvider(), d.getProviderMessageId(),
                    d.getStatus(), d.getAttempts(), d.getLastError(), d.getSentAt());
        }
    }

    public static NotificationDetailView from(Notification n, List<NotificationDelivery> deliveries) {
        return new NotificationDetailView(n.getId(), n.getRecipientUserId(), n.getType(), n.getTitle(),
                n.getBody(), n.getDataJson(), n.getCreatedAt(), n.getReadAt(),
                deliveries.stream().map(Delivery::from).toList());
    }
}
