package com.ruoyi.nadoumi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nadoumi.notification.domain.NotificationType;
import com.nadoumi.notification.service.NotificationRequest;
import com.nadoumi.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The notification read surface (Step 5, slice 2): a recipient sees only their
 * own feed at {@code /api/notifications}, can mark items read, and cannot touch
 * another user's rows; the staff oversight view at {@code /api/staff/notifications}
 * is permission-gated.
 */
class NotificationApiTest extends AbstractNadIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    private long notifyUser(long userId) {
        return notificationService.create(NotificationRequest.of(
                userId, NotificationType.SCHOLARSHIP_PUBLISHED,
                "Scholarship published", "A scholarship was published."));
    }

    @Test
    void recipient_seesOwnFeedAndUnreadCount_thenMarksItRead() throws Exception {
        long studentId = createStudent("notif_student");
        long otherId = createStudent("notif_other");
        long mine = notifyUser(studentId);
        notifyUser(otherId);
        String token = studentToken("notif_student");

        mvc.perform(get("/api/notifications").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value((int) mine))
                .andExpect(jsonPath("$.content[0].read").value(false));

        mvc.perform(get("/api/notifications/unread-count").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        mvc.perform(post("/api/notifications/{id}/read", mine).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(true));

        mvc.perform(get("/api/notifications/unread-count").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void recipient_cannotMarkAnotherUsersNotification() throws Exception {
        createStudent("notif_a");
        long userB = createStudent("notif_b");
        long bs = notifyUser(userB);
        String tokenA = studentToken("notif_a");

        mvc.perform(post("/api/notifications/{id}/read", bs).header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(false));
    }

    @Test
    void staffOversight_requiresThePermission() throws Exception {
        createStaff("notif_noperm", "case_officer"); // no nad:notification:* grant
        createStaff("notif_ok", "ops_manager");      // granted nad:notification:list by V33

        mvc.perform(get("/api/staff/notifications")
                        .header("Authorization", bearer(staffToken("notif_noperm"))))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/staff/notifications")
                        .header("Authorization", bearer(staffToken("notif_ok"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
