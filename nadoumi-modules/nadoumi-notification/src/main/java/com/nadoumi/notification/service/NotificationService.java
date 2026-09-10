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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    /** Chunk size for {@link #createBatch} — keeps each multi-row INSERT well under max_allowed_packet. */
    private static final int BATCH_CHUNK = 500;

    @Transactional(rollbackFor = Exception.class)
    public long create(NotificationRequest req) {
        validate(req);

        Notification n = rowFrom(req);
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

    /**
     * Bulk equivalent of {@link #create} for a fan-out to many recipients (a
     * published-catalog announcement to every active student). Each {@value #BATCH_CHUNK}-row
     * chunk is one multi-row notification insert + one multi-row delivery insert,
     * not N transactions of single-row inserts. Idempotent: a recipient whose
     * {@code (recipient, source_ref)} row already exists is skipped, so the outbox
     * poller's at-least-once redelivery stays a no-op.
     *
     * <p>All requests in a call must share one {@link NotificationType} (the outbox
     * dispatcher builds one batch per event).
     */
    @Transactional(rollbackFor = Exception.class)
    public void createBatch(List<NotificationRequest> reqs) {
        if (reqs == null || reqs.isEmpty()) {
            return;
        }
        for (int from = 0; from < reqs.size(); from += BATCH_CHUNK) {
            insertChunk(reqs.subList(from, Math.min(from + BATCH_CHUNK, reqs.size())));
        }
    }

    private void insertChunk(List<NotificationRequest> chunk) {
        chunk.forEach(NotificationService::validate);
        chunk.forEach(r -> Assert.hasText(r.sourceRef(), "createBatch requires a source_ref on every request"));

        List<String> refs = chunk.stream().map(NotificationRequest::sourceRef).toList();
        Set<String> existing = new HashSet<>(notificationMapper.findExistingSourceRefs(refs));
        List<Notification> rows = chunk.stream()
                .filter(r -> !existing.contains(r.sourceRef()))
                .map(NotificationService::rowFrom)
                .toList();
        if (rows.isEmpty()) {
            return;
        }
        notificationMapper.insertBatch(rows); // useGeneratedKeys back-fills each id

        NotificationType type = chunk.get(0).type();
        List<Long> recipientIds = rows.stream().map(Notification::getRecipientUserId).toList();

        List<NotificationDelivery> deliveries = new ArrayList<>(rows.size());
        LocalDateTime now = LocalDateTime.now();
        for (Notification n : rows) {
            deliveries.add(deliveryRow(n.getId(), NotificationChannelKind.IN_APP,
                    IN_APP_PROVIDER, DeliveryStatus.SENT, now));
        }
        for (NotificationChannelKind channel : type.secondaryChannels()) {
            Set<Long> optedOut = type.isTransactional() ? Set.of() : new HashSet<>(
                    preferenceMapper.findDisabledUserIds(recipientIds, type.name(), channel.name()));
            for (Notification n : rows) {
                if (!optedOut.contains(n.getRecipientUserId())) {
                    deliveries.add(deliveryRow(n.getId(), channel, null, DeliveryStatus.PENDING, null));
                }
            }
        }
        deliveryMapper.insertBatch(deliveries);
    }

    private static void validate(NotificationRequest req) {
        Assert.notNull(req.type(), "notification type is required");
        Assert.hasText(req.title(), "notification title is required");
        Assert.hasText(req.body(), "notification body is required");
    }

    private static Notification rowFrom(NotificationRequest req) {
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
        return n;
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

    /** Provider tag for an EMAIL row a specialised composer sent itself (not via the dispatcher). */
    private static final String EMAIL_DIRECT_PROVIDER = "mail";

    /**
     * Record the outcome of an EMAIL a specialised composer (e.g. the Welcome
     * email) sent directly through the {@code MailSender} port instead of the
     * generic dispatcher, so it still appears in the staff notification console.
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordDirectEmailDelivery(long notificationId, boolean sent, String error) {
        NotificationDelivery row = deliveryRow(notificationId, NotificationChannelKind.EMAIL,
                EMAIL_DIRECT_PROVIDER, sent ? DeliveryStatus.SENT : DeliveryStatus.FAILED,
                sent ? LocalDateTime.now() : null);
        if (!sent) {
            row.setLastError(error);
        }
        deliveryMapper.insert(row);
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
