package com.nadoumi.notification.service;

import com.nadoumi.common.erasure.AccountRetirementParticipant;
import com.nadoumi.notification.mapper.NotificationMapper;
import org.springframework.core.annotation.Order;

/** Removes the notifications addressed to a retired student account. */
@Order(40)
public class NotificationRetirement implements AccountRetirementParticipant {

    private final NotificationMapper notifications;

    public NotificationRetirement(NotificationMapper notifications) {
        this.notifications = notifications;
    }

    @Override
    public void retire(long userId) {
        notifications.removeByRecipient(userId);
    }
}
