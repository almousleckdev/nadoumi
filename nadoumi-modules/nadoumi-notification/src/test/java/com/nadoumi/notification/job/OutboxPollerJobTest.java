package com.nadoumi.notification.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.nadoumi.notification.domain.OutboxEvent;
import com.nadoumi.notification.mapper.OutboxEventMapper;
import com.nadoumi.notification.outbox.OutboxDispatcher;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Pure unit test — the mapper and dispatcher are mocks; nothing touches a database. */
class OutboxPollerJobTest {

    private final OutboxEventMapper mapper = mock(OutboxEventMapper.class);
    private final OutboxDispatcher dispatcher = mock(OutboxDispatcher.class);
    private final OutboxPollerJob job = new OutboxPollerJob(mapper, dispatcher);

    private static OutboxEvent event(long id, int retryCount) {
        OutboxEvent e = new OutboxEvent();
        e.setId(id);
        e.setAggregateType("scholarship");
        e.setAggregateId(id);
        e.setType("ScholarshipPublished");
        e.setPayloadJson("{}");
        e.setStatus("PENDING");
        e.setRetryCount(retryCount);
        return e;
    }

    @Test
    void marksEventDone_whenDispatchSucceeds() {
        when(mapper.findDispatchable(OutboxPollerJob.BATCH_SIZE)).thenReturn(List.of(event(7L, 0)));

        job.run();

        verify(dispatcher).dispatch(any(OutboxEvent.class));
        verify(mapper).markDone(eq(7L), any(LocalDateTime.class));
        verify(mapper, never()).recordFailure(anyLong(), any(), any(), any());
    }

    @Test
    void reschedulesWithBackoff_whenDispatchThrows() {
        when(mapper.findDispatchable(OutboxPollerJob.BATCH_SIZE)).thenReturn(List.of(event(9L, 0)));
        doThrow(new IllegalStateException("consumer down")).when(dispatcher).dispatch(any());

        LocalDateTime lower = LocalDateTime.now();
        job.run();

        ArgumentCaptor<String> status = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> error = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDateTime> next = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(mapper).recordFailure(eq(9L), status.capture(), error.capture(), next.capture());
        assertThat(status.getValue()).isEqualTo("PENDING");
        assertThat(error.getValue()).contains("consumer down");
        assertThat(next.getValue()).isAfter(lower.plusSeconds(20)); // first backoff step is 30s
        verify(mapper, never()).markDone(anyLong(), any());
    }

    @Test
    void parksEventFailed_whenRetriesAreExhausted() {
        when(mapper.findDispatchable(OutboxPollerJob.BATCH_SIZE))
                .thenReturn(List.of(event(11L, OutboxPollerJob.MAX_RETRIES - 1)));
        doThrow(new IllegalStateException("still down")).when(dispatcher).dispatch(any());

        job.run();

        verify(mapper).recordFailure(eq(11L), eq("FAILED"), any(), any(LocalDateTime.class));
    }

    @Test
    void doesNothing_whenOutboxIsEmpty() {
        when(mapper.findDispatchable(OutboxPollerJob.BATCH_SIZE)).thenReturn(List.of());

        job.run();

        verifyNoInteractions(dispatcher);
        verify(mapper, never()).markDone(anyLong(), any());
        verify(mapper, never()).recordFailure(anyLong(), any(), any(), any());
    }

    @Test
    void continuesBatch_whenOneEventFails() {
        when(mapper.findDispatchable(OutboxPollerJob.BATCH_SIZE))
                .thenReturn(List.of(event(1L, 0), event(2L, 0), event(3L, 0)));
        doThrow(new IllegalStateException("bad #2")).when(dispatcher).dispatch(argThatId(2L));

        job.run();

        verify(mapper).markDone(eq(1L), any());
        verify(mapper).recordFailure(eq(2L), eq("PENDING"), any(), any());
        verify(mapper).markDone(eq(3L), any());
    }

    private static OutboxEvent argThatId(long id) {
        return org.mockito.ArgumentMatchers.argThat(e -> e != null && e.getId() != null && e.getId() == id);
    }
}
