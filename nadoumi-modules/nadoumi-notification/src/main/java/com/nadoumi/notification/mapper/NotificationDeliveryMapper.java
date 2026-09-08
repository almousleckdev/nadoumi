package com.nadoumi.notification.mapper;

import com.nadoumi.notification.domain.NotificationDelivery;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** MyBatis mapper for {@code nad_notification_delivery}. */
public interface NotificationDeliveryMapper {

    /**
     * Enqueue one delivery. {@code createdAt} / {@code nextAttemptAt} default to
     * {@code now()} in SQL; {@code sentAt} is set only for an already-SENT row
     * (IN_APP). Relies on {@code uk_notif_delivery_channel} to stay idempotent.
     */
    void insert(NotificationDelivery delivery);

    /** Multi-row insert of already-built delivery rows (one fan-out chunk). */
    int insertBatch(@Param("rows") List<NotificationDelivery> rows);

    List<NotificationDelivery> findByNotification(@Param("notificationId") long notificationId);

    /** PENDING deliveries whose {@code next_attempt_at <= now()}, oldest first. */
    List<NotificationDelivery> findDispatchable(@Param("limit") int limit);

    int markSent(@Param("id") long id, @Param("provider") String provider,
            @Param("providerMessageId") String providerMessageId, @Param("sentAt") LocalDateTime sentAt);

    int recordFailure(@Param("id") long id, @Param("status") String status,
            @Param("lastError") String lastError, @Param("nextAttemptAt") LocalDateTime nextAttemptAt);

    long countByStatus(@Param("status") String status);
}
