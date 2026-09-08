package com.nadoumi.notification.mapper;

import com.nadoumi.notification.domain.NotificationPreference;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** MyBatis mapper for {@code nad_notification_preference}. */
public interface NotificationPreferenceMapper {

    /** {@code true}/{@code false} when a row exists for the triple, else {@code null}. */
    Boolean findEnabled(@Param("userId") long userId, @Param("type") String type,
            @Param("channel") String channel);

    /**
     * Of {@code userIds}, those that have explicitly turned this (type, channel)
     * off. Absence of a row means "enabled" (the default), so the complement is
     * the recipients a secondary-channel delivery should be enqueued for.
     */
    List<Long> findDisabledUserIds(@Param("userIds") Collection<Long> userIds,
            @Param("type") String type, @Param("channel") String channel);

    List<NotificationPreference> findByUser(@Param("userId") long userId);

    /** Insert or flip the {@code enabled} flag for the triple. */
    void upsert(@Param("userId") long userId, @Param("type") String type,
            @Param("channel") String channel, @Param("enabled") boolean enabled);
}
