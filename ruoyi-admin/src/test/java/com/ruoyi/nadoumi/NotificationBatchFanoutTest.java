package com.ruoyi.nadoumi;

import static org.assertj.core.api.Assertions.assertThat;

import com.nadoumi.notification.job.OutboxPollerJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A published-catalog event fans out to every staff recipient + every active
 * student through {@code NotificationService.createBatch} (one multi-row insert
 * per chunk, not one transaction per recipient). Re-running the poller must not
 * double-notify — the {@code source_ref} pre-filter makes redelivery a no-op.
 */
class NotificationBatchFanoutTest extends AbstractNadIntegrationTest {

    @Autowired
    private OutboxPollerJob outboxPollerJob;

    @Test
    void universityPublished_fansOutOncePerRecipient_andIsIdempotent() {
        long staffId = createStaff("fan_staff", "ops_manager"); // holds nad:notification:list
        long s1 = createStudent("fan_s1");
        long s2 = createStudent("fan_s2");

        jdbc.update("""
                insert into nad_outbox_event
                    (aggregate_type, aggregate_id, type, payload_json, status, retry_count, created_at, next_attempt_at)
                values ('university', 1, 'UniversityPublished',
                        '{"universityId":1,"universityName":"Test U","country":"CN","universitySlug":"test-u"}',
                        'PENDING', 0, now(), now())""");

        outboxPollerJob.run();

        int after1 = countUniversityPublished();
        // the three recipients I created each got exactly one row, IN_APP delivered
        for (long uid : new long[] { staffId, s1, s2 }) {
            assertThat(jdbc.queryForObject(
                    "select count(*) from nad_notification where recipient_user_id = ? and type = 'UNIVERSITY_PUBLISHED'",
                    Integer.class, uid)).isEqualTo(1);
            assertThat(jdbc.queryForObject(
                    "select d.status from nad_notification_delivery d join nad_notification n on n.id = d.notification_id "
                            + "where n.recipient_user_id = ? and n.type = 'UNIVERSITY_PUBLISHED' and d.channel = 'IN_APP'",
                    String.class, uid)).isEqualTo("SENT");
        }

        // redeliver the same event — the source_ref pre-filter must make it a no-op
        jdbc.update("update nad_outbox_event set status = 'PENDING', next_attempt_at = now() "
                + "where type = 'UniversityPublished'");
        outboxPollerJob.run();

        assertThat(countUniversityPublished()).isEqualTo(after1);
    }

    private int countUniversityPublished() {
        return jdbc.queryForObject(
                "select count(*) from nad_notification where type = 'UNIVERSITY_PUBLISHED'", Integer.class);
    }
}
