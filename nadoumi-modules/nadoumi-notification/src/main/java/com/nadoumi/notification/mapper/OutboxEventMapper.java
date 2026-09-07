package com.nadoumi.notification.mapper;

import com.nadoumi.notification.domain.OutboxEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper for {@code nad_outbox_event}. Picked up by the
 * {@code com.nadoumi.**.mapper} scan in {@code NadoumiModuleConfiguration}, so no
 * {@code @Mapper} annotation.
 */
public interface OutboxEventMapper {

    /** Persist a new PENDING event; sets the generated {@code id} back on {@code event}. */
    void insert(OutboxEvent event);

    OutboxEvent findById(@Param("id") long id);

    /**
     * The next batch of events ready to dispatch: {@code status = 'PENDING'} and
     * {@code next_attempt_at <= now()}, oldest first, at most {@code limit} rows.
     */
    List<OutboxEvent> findDispatchable(@Param("limit") int limit);

    /** Mark an event delivered. */
    int markDone(@Param("id") long id, @Param("processedAt") LocalDateTime processedAt);

    /**
     * Record a failed attempt: increment {@code retry_count}, store
     * {@code last_error}, move to {@code status} (PENDING to retry, or FAILED once
     * dead) and push {@code next_attempt_at} out for backoff.
     */
    int recordFailure(@Param("id") long id, @Param("status") String status,
            @Param("lastError") String lastError, @Param("nextAttemptAt") LocalDateTime nextAttemptAt);

    /** Count rows in a given status — for tests and operational checks. */
    long countByStatus(@Param("status") String status);
}
