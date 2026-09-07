package com.ruoyi.nadoumi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nadoumi.notification.job.NotificationDispatchJob;
import com.nadoumi.notification.job.OutboxPollerJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Full Step 5 chain: a public contact submission writes {@code ContactInquiryReceived}
 * to the outbox in the same transaction; the poller turns it into a
 * {@code nad_notification} per support-staff recipient (IN_APP delivered at once,
 * EMAIL queued); the dispatch job sends the EMAIL through the {@code MailSender}
 * port (the no-network {@code log} adapter here).
 */
class NotificationPipelineTest extends AbstractNadIntegrationTest {

    @Autowired
    private OutboxPollerJob outboxPollerJob;

    @Autowired
    private NotificationDispatchJob notificationDispatchJob;

    @Test
    void contactSubmission_flowsThroughOutboxToAnInAppAndEmailNotification() throws Exception {
        long staffId = createStaff("pipe_staff", "ops_manager"); // holds nad:notification:list

        mvc.perform(post("/api/public/contact").contentType("application/json").content("""
                {"firstName":"Dana","lastName":"Ali","email":"dana@example.com",
                 "category":"SCHOLARSHIPS","subject":"Question","message":"Is the intake open?","locale":"en"}"""))
                .andExpect(status().isAccepted());

        assertThat(jdbc.queryForObject(
                "select count(*) from nad_outbox_event where type = 'ContactInquiryReceived' and status = 'PENDING'",
                Integer.class)).isEqualTo(1);

        outboxPollerJob.run();

        assertThat(jdbc.queryForObject(
                "select count(*) from nad_outbox_event where type = 'ContactInquiryReceived' and status = 'DONE'",
                Integer.class)).isEqualTo(1);

        Long notificationId = jdbc.queryForObject(
                "select id from nad_notification where recipient_user_id = ? and type = 'CONTACT_INQUIRY_RECEIVED'",
                Long.class, staffId);
        assertThat(notificationId).isNotNull();
        assertThat(jdbc.queryForObject(
                "select source_ref from nad_notification where id = ?", String.class, notificationId))
                .startsWith("outbox:");

        assertThat(jdbc.queryForObject(
                "select status from nad_notification_delivery where notification_id = ? and channel = 'IN_APP'",
                String.class, notificationId)).isEqualTo("SENT");
        assertThat(jdbc.queryForObject(
                "select status from nad_notification_delivery where notification_id = ? and channel = 'EMAIL'",
                String.class, notificationId)).isEqualTo("PENDING");

        notificationDispatchJob.run();

        assertThat(jdbc.queryForObject(
                "select status from nad_notification_delivery where notification_id = ? and channel = 'EMAIL'",
                String.class, notificationId)).isEqualTo("SENT");
    }
}
