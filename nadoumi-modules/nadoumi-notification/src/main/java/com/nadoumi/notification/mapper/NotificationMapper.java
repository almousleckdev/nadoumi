package com.nadoumi.notification.mapper;

import com.nadoumi.notification.domain.Notification;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper for {@code nad_notification}. Picked up by the
 * {@code com.nadoumi.**.mapper} scan; no {@code @Mapper} annotation.
 */
public interface NotificationMapper {

    /** @return rows inserted — {@code 0} when a same-{@code (recipient, source_ref)} row already exists. */
    int insert(Notification notification);

    /**
     * Which of the given {@code source_ref}s already have a row — used to skip
     * already-delivered recipients before a batch insert (idempotency, without an
     * {@code INSERT IGNORE} that would break generated-key back-fill).
     */
    List<String> findExistingSourceRefs(@Param("sourceRefs") Collection<String> sourceRefs);

    /**
     * Multi-row insert; {@code id} is back-filled into each element. Callers must
     * pass only rows whose {@code source_ref} is not already present
     * (see {@link #findExistingSourceRefs}).
     */
    int insertBatch(@Param("rows") List<Notification> rows);

    /** One recipient's feed, newest first; {@code unreadOnly} restricts to {@code read_at IS NULL}. */
    List<Notification> findByRecipient(@Param("recipientUserId") long recipientUserId,
            @Param("unreadOnly") boolean unreadOnly);

    long countUnread(@Param("recipientUserId") long recipientUserId);

    /** Load a notification only if it belongs to {@code recipientUserId} (ownership guard). */
    Notification findByIdForRecipient(@Param("id") long id, @Param("recipientUserId") long recipientUserId);

    int markRead(@Param("id") long id, @Param("recipientUserId") long recipientUserId);

    int markAllRead(@Param("recipientUserId") long recipientUserId);

    /** Staff view — optional filters on recipient and type. */
    List<Notification> search(@Param("recipientUserId") Long recipientUserId, @Param("type") String type);

    Notification findById(@Param("id") long id);
}
