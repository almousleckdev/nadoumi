package com.nadoumi.notification.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * Minimal read of {@code sys_user} for delivery — the notification module resolves
 * a recipient's address without depending on {@code ruoyi-system}'s services.
 */
public interface NotificationRecipientMapper {

    /** The recipient's email, or {@code null} when the user is missing / soft-deleted / has none. */
    String findEmail(@Param("userId") long userId);
}
