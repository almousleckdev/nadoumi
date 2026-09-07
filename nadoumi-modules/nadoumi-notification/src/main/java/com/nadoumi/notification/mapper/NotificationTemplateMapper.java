package com.nadoumi.notification.mapper;

import com.nadoumi.notification.domain.NotificationTemplate;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** MyBatis mapper for {@code nad_notification_template}. */
public interface NotificationTemplateMapper {

    /** Exact {@code (type, channel, locale)} match, or {@code null}. */
    NotificationTemplate find(@Param("type") String type, @Param("channel") String channel,
            @Param("locale") String locale);

    List<NotificationTemplate> findAll();

    NotificationTemplate findById(@Param("id") long id);

    int update(NotificationTemplate template);
}
