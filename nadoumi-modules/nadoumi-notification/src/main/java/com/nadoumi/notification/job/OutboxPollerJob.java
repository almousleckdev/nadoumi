package com.nadoumi.notification.job;

import com.nadoumi.notification.domain.OutboxEvent;
import com.nadoumi.notification.domain.OutboxStatus;
import com.nadoumi.notification.mapper.OutboxEventMapper;
import com.nadoumi.notification.outbox.OutboxDispatcher;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drains {@code nad_outbox_event} into the notification pipeline. Invoked by RuoYi
 * Quartz by bean name from a {@code sys_job} row
 * ({@code invoke_target = 'outboxPollerJob.run()'}, seeded active by V31); the
 * bean is wired in {@code NotificationAutoConfiguration}.
 *
 * <p>RuoYi's clustered Quartz store fires this on one instance at a time, so the
 * sweep does not need row-level fencing. Each event is dispatched independently:
 * a failure is recorded ({@code retry_count++}, {@code last_error}, exponential-ish
 * backoff on {@code next_attempt_at}) and retried on a later sweep, up to
 * {@link #MAX_RETRIES} attempts before the row is parked {@code FAILED} and an
 * error is logged for operational follow-up. Logs counts and event types only —
 * never the payload body ({@code docs/SECURITY.md} §6).</p>
 */
public class OutboxPollerJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxPollerJob.class);

    /** Rows handled per sweep. The poller runs often (V31 cron), so this stays modest. */
    static final int BATCH_SIZE = 100;

    /** Attempts before a row is parked {@code FAILED}. */
    static final int MAX_RETRIES = 10;

    /** Backoff step; delay is {@code min(retryCount * STEP, CAP)}. */
    private static final long BACKOFF_STEP_SECONDS = 30L;
    private static final long BACKOFF_CAP_SECONDS = 900L;

    private static final int LAST_ERROR_MAX_CHARS = 500;

    private final OutboxEventMapper mapper;
    private final OutboxDispatcher dispatcher;

    public OutboxPollerJob(OutboxEventMapper mapper, OutboxDispatcher dispatcher) {
        this.mapper = mapper;
        this.dispatcher = dispatcher;
    }

    /** Quartz entry point. */
    public void run() {
        List<OutboxEvent> batch = mapper.findDispatchable(BATCH_SIZE);
        if (batch.isEmpty()) {
            return;
        }

        int dispatched = 0;
        int retrying = 0;
        int dead = 0;
        for (OutboxEvent event : batch) {
            try {
                dispatcher.dispatch(event);
                mapper.markDone(event.getId(), LocalDateTime.now());
                dispatched++;
            } catch (RuntimeException e) {
                int attempts = event.getRetryCount() + 1;
                boolean giveUp = attempts >= MAX_RETRIES;
                String status = giveUp ? OutboxStatus.FAILED.name() : OutboxStatus.PENDING.name();
                mapper.recordFailure(event.getId(), status, truncate(e.toString()), nextAttempt(attempts));
                if (giveUp) {
                    dead++;
                    log.error("outbox event id={} type={} parked FAILED after {} attempts: {}",
                            event.getId(), event.getType(), attempts, e.toString());
                } else {
                    retrying++;
                    log.warn("outbox event id={} type={} attempt {} failed, will retry: {}",
                            event.getId(), event.getType(), attempts, e.toString());
                }
            }
        }

        log.info("outbox poll: scanned={} dispatched={} retrying={} dead={}",
                batch.size(), dispatched, retrying, dead);
    }

    private static LocalDateTime nextAttempt(int attempts) {
        long delay = Math.min(attempts * BACKOFF_STEP_SECONDS, BACKOFF_CAP_SECONDS);
        return LocalDateTime.now().plusSeconds(delay);
    }

    private static String truncate(String s) {
        return s.length() <= LAST_ERROR_MAX_CHARS ? s : s.substring(0, LAST_ERROR_MAX_CHARS);
    }
}
