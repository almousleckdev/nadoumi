package com.ruoyi.nadoumi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nadoumi.common.outbox.OutboxEventTypes;
import com.nadoumi.common.outbox.OutboxWriter;
import com.nadoumi.notification.job.OutboxPollerJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * End-to-end check of the transactional outbox (Step 5, slice 1): a producer
 * writes an event on its own transaction, and {@link OutboxPollerJob} drains it to
 * {@code DONE}. Also proves the {@code MANDATORY} propagation contract — a write
 * with no active transaction is refused and nothing is persisted.
 *
 * <p>The dispatcher wired here is {@code LoggingOutboxDispatcher} (slice 1
 * default), which always succeeds; the mapping to real notifications is exercised
 * in a later slice.</p>
 */
class OutboxPipelineTest extends AbstractNadIntegrationTest {

    @Autowired
    private OutboxWriter outboxWriter;

    @Autowired
    private OutboxPollerJob outboxPollerJob;

    @Autowired
    private PlatformTransactionManager txManager;

    private TransactionTemplate tx;

    @BeforeEach
    void clearOutbox() {
        tx = new TransactionTemplate(txManager);
        jdbc.update("delete from nad_outbox_event");
    }

    @Test
    void poller_drainsAWrittenEventToDone() {
        // super-admin almousleck (user 3) holds nad:notification:list, so the
        // dispatcher renders the SCHOLARSHIP_PUBLISHED template — give it its vars.
        String payload = "{\"scholarshipId\":4321,\"scholarshipTitle\":\"Test Grant\","
                + "\"scholarshipReference\":\"NAC-2026-0001\"}";
        long id = tx.execute(status -> outboxWriter.write(
                "scholarship", 4321L, OutboxEventTypes.SCHOLARSHIP_PUBLISHED, payload));

        Integer rows = jdbc.queryForObject(
                "select count(*) from nad_outbox_event where id = ?", Integer.class, id);
        assertThat(rows).isEqualTo(1);

        outboxPollerJob.run();

        String statusValue = jdbc.queryForObject(
                "select status from nad_outbox_event where id = ?", String.class, id);
        Integer processed = jdbc.queryForObject(
                "select count(*) from nad_outbox_event where id = ? and processed_at is not null",
                Integer.class, id);
        assertThat(statusValue).isEqualTo("DONE");
        assertThat(processed).isEqualTo(1);
    }

    @Test
    void outboxWriter_rejectsAWriteWithNoActiveTransaction() {
        assertThatThrownBy(() -> outboxWriter.write(
                "scholarship", 999L, OutboxEventTypes.SCHOLARSHIP_PUBLISHED, "{}"))
                .isInstanceOf(IllegalTransactionStateException.class);

        Integer rows = jdbc.queryForObject(
                "select count(*) from nad_outbox_event where aggregate_id = 999", Integer.class);
        assertThat(rows).isZero();
    }
}
