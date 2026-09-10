package com.nadoumi.notification.dispatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import com.nadoumi.common.notification.NotificationChannel;
import com.nadoumi.common.notification.NotificationChannelKind;
import com.nadoumi.common.notification.NotificationSendResult;
import com.nadoumi.notification.domain.Notification;
import com.nadoumi.notification.domain.NotificationDelivery;
import com.nadoumi.notification.mapper.NotificationDeliveryMapper;
import com.nadoumi.notification.mapper.NotificationMapper;
import com.nadoumi.notification.mapper.NotificationRecipientMapper;
import com.nadoumi.notification.render.NotificationRenderer;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NotificationDeliveryDispatcherTest {

    private final NotificationDeliveryMapper deliveryMapper = mock(NotificationDeliveryMapper.class);
    private final NotificationMapper notificationMapper = mock(NotificationMapper.class);
    private final NotificationRecipientMapper recipientMapper = mock(NotificationRecipientMapper.class);
    private final NotificationRenderer renderer = mock(NotificationRenderer.class);
    private final NotificationChannel emailChannel = mock(NotificationChannel.class);

    private NotificationDeliveryDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        when(emailChannel.kind()).thenReturn(NotificationChannelKind.EMAIL);
        when(renderer.render(any(), any(), any(), any()))
                .thenReturn(new NotificationRenderer.Rendered("subject", "body"));
        dispatcher = new NotificationDeliveryDispatcher(
                deliveryMapper, notificationMapper, recipientMapper, renderer, List.of(emailChannel));
    }

    private static NotificationDelivery pending(long id, int attempts) {
        NotificationDelivery d = new NotificationDelivery();
        d.setId(id);
        d.setNotificationId(100L + id);
        d.setChannel("EMAIL");
        d.setStatus("PENDING");
        d.setAttempts(attempts);
        return d;
    }

    private static Notification notification(long id) {
        Notification n = new Notification();
        n.setId(id);
        n.setRecipientUserId(9L);
        n.setType("SCHOLARSHIP_PUBLISHED");
        n.setDataJson("{\"scholarshipTitle\":\"X\",\"scholarshipReference\":\"NAC-2026-0001\"}");
        return n;
    }

    @Test
    void marksDeliverySent_whenTheChannelAccepts() {
        when(deliveryMapper.findDispatchable(100)).thenReturn(List.of(pending(1L, 0)));
        when(notificationMapper.findById(101L)).thenReturn(notification(101L));
        when(recipientMapper.findEmail(9L)).thenReturn("stu@example.test");
        when(emailChannel.send(eq("stu@example.test"), any(), any(), any()))
                .thenReturn(NotificationSendResult.sent(null));

        dispatcher.run();

        verify(deliveryMapper).markSent(eq(1L), eq("mail"), any(), any(LocalDateTime.class));
        verify(deliveryMapper, never()).recordFailure(anyLong(), any(), any(), any());
    }

    @Test
    void reschedulesWithBackoff_whenTheChannelFails() {
        when(deliveryMapper.findDispatchable(100)).thenReturn(List.of(pending(2L, 0)));
        when(notificationMapper.findById(102L)).thenReturn(notification(102L));
        when(recipientMapper.findEmail(9L)).thenReturn("stu@example.test");
        when(emailChannel.send(any(), any(), any(), any())).thenReturn(NotificationSendResult.failed("smtp down"));

        dispatcher.run();

        ArgumentCaptor<String> status = ArgumentCaptor.forClass(String.class);
        verify(deliveryMapper).recordFailure(eq(2L), status.capture(), eq("smtp down"), any(LocalDateTime.class));
        assertThat(status.getValue()).isEqualTo("PENDING");
        verify(deliveryMapper, never()).markSent(anyLong(), any(), any(), any());
    }

    @Test
    void parksFailed_whenAttemptsAreExhausted() {
        when(deliveryMapper.findDispatchable(100))
                .thenReturn(List.of(pending(3L, NotificationDeliveryDispatcher.MAX_ATTEMPTS - 1)));
        when(notificationMapper.findById(103L)).thenReturn(notification(103L));
        when(recipientMapper.findEmail(9L)).thenReturn("stu@example.test");
        when(emailChannel.send(any(), any(), any(), any())).thenReturn(NotificationSendResult.failed("still down"));

        dispatcher.run();

        verify(deliveryMapper).recordFailure(eq(3L), eq("FAILED"), any(), any(LocalDateTime.class));
    }

    @Test
    void parksFailedPermanently_whenTheRecipientHasNoEmail() {
        when(deliveryMapper.findDispatchable(100)).thenReturn(List.of(pending(4L, 0)));
        when(notificationMapper.findById(104L)).thenReturn(notification(104L));
        when(recipientMapper.findEmail(9L)).thenReturn(null);

        dispatcher.run();

        verify(deliveryMapper).recordFailure(eq(4L), eq("FAILED"), any(), any(LocalDateTime.class));
        verify(emailChannel, never()).send(any(), any(), any(), any());
    }
}
