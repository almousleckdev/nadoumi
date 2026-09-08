package com.nadoumi.notification.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nadoumi.common.notification.NotificationChannelKind;
import com.nadoumi.common.web.PageResponse;
import com.nadoumi.common.web.PageSupport;
import com.nadoumi.notification.domain.DeliveryStatus;
import com.nadoumi.notification.domain.Notification;
import com.nadoumi.notification.domain.NotificationDelivery;
import com.nadoumi.notification.domain.NotificationType;
import com.nadoumi.notification.mapper.NotificationDeliveryMapper;
import com.nadoumi.notification.mapper.NotificationMapper;
import com.nadoumi.notification.mapper.NotificationPreferenceMapper;
import com.nadoumi.notification.web.response.NotificationDetailView;
import com.nadoumi.notification.web.response.NotificationView;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * Creates notifications and fans them out to channels. One
 * {@link #create(NotificationRequest)} writes one {@code nad_notification} row and
 * enqueues deliveries: {@code IN_APP} always (recorded {@code SENT} at once — the
 * row itself is the delivery), plus each of the type's
 * {@link NotificationType#secondaryChannels() secondary channels} whose preference
 * allows it. Transactional types ignore the preference table.
 *
 * <p>Async channel dispatch (EMAIL) and the retry sweep land in slice 3; this
 * class only enqueues.</p>
 */
public class NotificationService {

    private static final String IN_APP_PROVIDER = "IN_APP";

    private final NotificationMapper notificationMapper;
    private final NotificationDeliveryMapper deliveryMapper;
    private final NotificationPreferenceMapper preferenceMapper;

    public NotificationService(NotificationMapper notificationMapper, NotificationDeliveryMapper deliveryMapper,
            NotificationPreferenceMapper preferenceMapper) {
        this.notificationMapper = notificationMapper;
        this.deliveryMapper = deliveryMapper;
        this.preferenceMapper = preferenceMapper;
    }

    /**
     * @return the new {@code nad_notification} id
     */
    @Transactional(rollbackFor = Exception.class)
    public long create(NotificationRequest req) {
        Assert.notNull(req.type(), "notification type is required");
        Assert.hasText(req.title(), "notification title is required");
        Assert.hasText(req.body(), "notification body is required");

        Notification n = new Notification();
        n.setRecipientUserId(req.recipientUserId());
        n.setType(req.type().name());
        n.setTitle(req.title());
        n.setBody(req.body());
        n.setDataJson(req.dataJson());
        n.setSourceRef(req.sourceRef());
        n.setApplicationId(req.applicationId());
        n.setConversationId(req.conversationId());
        n.setMessageId(req.messageId());
        int inserted = notificationMapper.insert(n);
        if (inserted == 0 || n.getId() == null) {
            return 0L; // a row for this (recipient, source_ref) already exists — idempotent no-op
        }

        deliveryMapper.insert(deliveryRow(n.getId(), NotificationChannelKind.IN_APP,
                IN_APP_PROVIDER, DeliveryStatus.SENT, LocalDateTime.now()));

        for (NotificationChannelKind channel : req.type().secondaryChannels()) {
            if (channelEnabled(req.recipientUserId(), req.type(), channel)) {
                deliveryMapper.insert(deliveryRow(n.getId(), channel, null, DeliveryStatus.PENDING, null));
            }
        }
        return n.getId();
    }

    // ---- recipient reads --------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<NotificationView> listForRecipient(long userId, boolean unreadOnly, int page, int size) {
        page = PageSupport.clampPage(page);
        size = PageSupport.clampSize(size);
        PageHelper.startPage(page + 1, size);
        List<Notification> rows = notificationMapper.findByRecipient(userId, unreadOnly);
        long total = new PageInfo<>(rows).getTotal();
        return PageResponse.of(rows.stream().map(NotificationView::from).toList(), page, size, total);
    }

    @Transactional(readOnly = true)
    public long unreadCount(long userId) {
        return notificationMapper.countUnread(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean markRead(long id, long userId) {
        return notificationMapper.markRead(id, userId) > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public int markAllRead(long userId) {
        return notificationMapper.markAllRead(userId);
    }

    // ---- staff reads ----------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<NotificationView> staffSearch(Long recipientUserId, String type, int page, int size) {
        page = PageSupport.clampPage(page);
        size = PageSupport.clampSize(size);
        PageHelper.startPage(page + 1, size);
        List<Notification> rows = notificationMapper.search(recipientUserId, type);
        long total = new PageInfo<>(rows).getTotal();
        return PageResponse.of(rows.stream().map(NotificationView::from).toList(), page, size, total);
    }

    @Transactional(readOnly = true)
    public NotificationDetailView staffGet(long id) {
        Notification n = notificationMapper.findById(id);
        if (n == null) {
            return null;
        }
        return NotificationDetailView.from(n, deliveryMapper.findByNotification(id));
    }

    private boolean channelEnabled(long userId, NotificationType type, NotificationChannelKind channel) {
        if (type.isTransactional()) {
            return true;
        }
        Boolean pref = preferenceMapper.findEnabled(userId, type.name(), channel.name());
        return pref == null || pref;
    }

    private static NotificationDelivery deliveryRow(long notificationId, NotificationChannelKind channel,
            String provider, DeliveryStatus status, LocalDateTime sentAt) {
        NotificationDelivery d = new NotificationDelivery();
        d.setNotificationId(notificationId);
        d.setChannel(channel.name());
        d.setProvider(provider);
        d.setStatus(status.name());
        d.setSentAt(sentAt);
        return d;
    }
}
