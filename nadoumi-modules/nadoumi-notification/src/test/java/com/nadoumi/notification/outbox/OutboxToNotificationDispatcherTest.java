package com.nadoumi.notification.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.common.notification.NotificationChannelKind;
import com.nadoumi.common.outbox.OutboxEventTypes;
import com.nadoumi.notification.domain.NotificationType;
import com.nadoumi.notification.domain.OutboxEvent;
import com.nadoumi.notification.mapper.NotificationAudienceMapper;
import com.nadoumi.notification.render.NotificationRenderer;
import com.nadoumi.notification.service.NotificationRequest;
import com.nadoumi.notification.service.NotificationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OutboxToNotificationDispatcherTest {

    private final NotificationService notificationService = mock(NotificationService.class);
    private final NotificationRenderer renderer = mock(NotificationRenderer.class);
    private final NotificationAudienceMapper audience = mock(NotificationAudienceMapper.class);
    private final WelcomeContentComposer welcomeComposer = mock(WelcomeContentComposer.class);
    private final OutboxToNotificationDispatcher dispatcher =
            new OutboxToNotificationDispatcher(notificationService, renderer, audience, welcomeComposer);

    private static OutboxEvent event(String type, String payload) {
        OutboxEvent e = new OutboxEvent();
        e.setId(42L);
        e.setType(type);
        e.setPayloadJson(payload);
        return e;
    }

    @Test
    void universityPublished_notifiesStaffAudienceAndEveryActiveStudent() {
        when(audience.findStaffUserIdsWithPermission("nad:notification:list")).thenReturn(List.of(1L));
        when(audience.findActiveStudentUserIds()).thenReturn(List.of(10L, 11L));
        when(renderer.render(eq(NotificationType.UNIVERSITY_PUBLISHED), eq(NotificationChannelKind.IN_APP),
                anyString(), any())).thenReturn(new NotificationRenderer.Rendered(null, "New university"));

        dispatcher.dispatch(event(OutboxEventTypes.UNIVERSITY_PUBLISHED,
                "{\"universityId\":7,\"universityName\":\"Fudan\",\"country\":\"CN\",\"universitySlug\":\"fudan\"}"));

        // one staff + two students, fanned out in a single batch call
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<NotificationRequest>> batch = ArgumentCaptor.forClass(List.class);
        verify(notificationService).createBatch(batch.capture());
        assertThat(batch.getValue()).extracting(NotificationRequest::recipientUserId)
                .containsExactlyInAnyOrder(1L, 10L, 11L);
        assertThat(batch.getValue()).extracting(NotificationRequest::sourceRef)
                .allMatch(ref -> ref.startsWith("outbox:42:"));
    }

    @Test
    void taskProgress_usesTheExplicitRecipientListAndSkipsStudents() {
        when(renderer.render(eq(NotificationType.TASK_PROGRESS), eq(NotificationChannelKind.IN_APP),
                anyString(), any())).thenReturn(new NotificationRenderer.Rendered(null, "Task moved"));

        dispatcher.dispatch(event(OutboxEventTypes.TASK_PROGRESS_CHANGED,
                "{\"taskId\":3,\"taskTitle\":\"x\",\"taskStatus\":\"IN_PROGRESS\",\"fromStatus\":\"PENDING\","
                        + "\"actor\":\"a\",\"recipientUserIds\":[5,6]}"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<NotificationRequest>> batch = ArgumentCaptor.forClass(List.class);
        verify(notificationService).createBatch(batch.capture());
        assertThat(batch.getValue()).extracting(NotificationRequest::recipientUserId)
                .containsExactlyInAnyOrder(5L, 6L);
        verify(audience, times(0)).findActiveStudentUserIds();
    }

    @Test
    void studentRegistered_isHandedToTheWelcomeComposer_notTheGenericFanOut() {
        OutboxEvent e = event(OutboxEventTypes.STUDENT_REGISTERED,
                "{\"userId\":77,\"email\":\"stu@example.test\",\"firstName\":\"Amina\",\"locale\":\"en\"}");

        dispatcher.dispatch(e);

        ArgumentCaptor<java.util.Map<String, Object>> ctx = ArgumentCaptor.forClass(java.util.Map.class);
        verify(welcomeComposer).handle(eq(e), ctx.capture());
        assertThat(ctx.getValue()).containsEntry("userId", 77).containsEntry("email", "stu@example.test");
        verify(notificationService, times(0)).create(any());
        verify(notificationService, times(0)).createBatch(any());
    }
}
