package com.nadoumi.notification.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * Resolves who should receive an event-driven notification. Reads RuoYi's RBAC
 * tables directly (read-only) rather than depending on {@code ruoyi-system}.
 */
public interface NotificationAudienceMapper {

    /**
     * Active staff ({@code user_type = '00'}, not deleted, status normal) who hold
     * {@code perm} through any assigned role.
     */
    List<Long> findStaffUserIdsWithPermission(@Param("perm") String perm);

    /**
     * Active registered students ({@code user_type = '10'}, not deleted, status
     * normal). Audience for public-catalog announcements.
     */
    List<Long> findActiveStudentUserIds();
}
