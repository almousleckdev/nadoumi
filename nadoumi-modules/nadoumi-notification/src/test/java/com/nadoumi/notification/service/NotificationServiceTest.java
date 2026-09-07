package com.nadoumi.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.notification.domain.Notification;
import com.nadoumi.notification.domain.NotificationDelivery;
import com.nadoumi.notification.domain.NotificationType;
import com.nadoumi.notification.mapper.NotificationDeliveryMapper;
import com.nadoumi.notification.mapper.NotificationMapper;
import com.nadoumi.notification.mapper.NotificationPreferenceMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Pure unit test — the three mappers are mocks; nothing touches a database. */
class NotificationServiceTest {

    private final NotificationMapper notificationMapper = mock(NotificationMapper.class);
    private final NotificationDeliveryMapper deliveryMapper = mock(NotificationDeliveryMapper.class);
    private final NotificationPreferenceMapper preferenceMapper = mock(NotificationPreferenceMapper.class);
    private final NotificationService service =
            new NotificationService(notificationMapper, deliveryMapper, preferenceMapper);

    NotificationServiceTest() {
        // stamp a generated id the way MyBatis useGeneratedKeys would, and report 1 row
        doAnswer(inv -> {
            inv.getArgument(0, Notification.class).setId(42L);
            return 1;
        }).when(notificationMapper).insert(any(Notification.class));
    }

    private static NotificationRequest contactInquiry() {
        return NotificationRequest.of(7L, NotificationType.CONTACT_INQUIRY_RECEIVED, "t", "b");
    }

    private static NotificationRequest scholarshipPublished() {
        return NotificationRequest.of(7L, NotificationType.SCHOLARSHIP_PUBLISHED, "t", "b");
    }

    private List<NotificationDelivery> capturedDeliveries(int expectedInserts) {
        ArgumentCaptor<NotificationDelivery> captor = ArgumentCaptor.forClass(NotificationDelivery.class);
        verify(deliveryMapper, times(expectedInserts)).insert(captor.capture());
        return captor.getAllValues();
    }

    @Test
    void create_writesNotificationAndAnInAppDeliveryMarkedSent() {
        long id = service.create(contactInquiry());

        assertThat(id).isEqualTo(42L);
        NotificationDelivery inApp = capturedDeliveries(2).stream()
                .filter(d -> "IN_APP".equals(d.getChannel())).findFirst().orElseThrow();
        assertThat(inApp.getStatus()).isEqualTo("SENT");
        assertThat(inApp.getSentAt()).isNotNull();
    }

    @Test
    void create_enqueuesEmail_forTransactionalType_ignoringDisabledPreference() {
        when(preferenceMapper.findEnabled(eq(7L), anyString(), eq("EMAIL"))).thenReturn(false);

        service.create(contactInquiry()); // CONTACT_INQUIRY_RECEIVED is transactional

        assertThat(capturedDeliveries(2))
                .anyMatch(d -> "EMAIL".equals(d.getChannel()) && "PENDING".equals(d.getStatus()));
    }

    @Test
    void create_skipsEmail_whenPreferenceDisabled_forNonTransactionalType() {
        when(preferenceMapper.findEnabled(7L, "SCHOLARSHIP_PUBLISHED", "EMAIL")).thenReturn(false);

        service.create(scholarshipPublished());

        assertThat(capturedDeliveries(1))
                .singleElement()
                .satisfies(d -> assertThat(d.getChannel()).isEqualTo("IN_APP"));
    }

    @Test
    void create_enqueuesEmail_whenNoPreferenceRow_forNonTransactionalType() {
        when(preferenceMapper.findEnabled(anyLong(), anyString(), anyString())).thenReturn(null);

        service.create(scholarshipPublished());

        assertThat(capturedDeliveries(2))
                .anyMatch(d -> "EMAIL".equals(d.getChannel()) && "PENDING".equals(d.getStatus()));
    }
}
