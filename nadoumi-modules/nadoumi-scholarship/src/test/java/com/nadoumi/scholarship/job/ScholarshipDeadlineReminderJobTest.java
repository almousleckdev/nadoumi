package com.nadoumi.scholarship.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.common.outbox.OutboxEventTypes;
import com.nadoumi.common.outbox.OutboxWriter;
import com.nadoumi.scholarship.domain.Scholarship;
import com.nadoumi.scholarship.mapper.ScholarshipMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ScholarshipDeadlineReminderJobTest {

    private final ScholarshipMapper mapper = mock(ScholarshipMapper.class);
    private final OutboxWriter outbox = mock(OutboxWriter.class);
    private final ScholarshipDeadlineReminderJob job = new ScholarshipDeadlineReminderJob(mapper, outbox);

    private static Scholarship due(long id, int daysOut) {
        Scholarship s = new Scholarship();
        s.setId(id);
        s.setTitle("CSC Master Scholarship");
        s.setSlug("csc-master");
        s.setReferenceCode("NAC-2026-0007");
        s.setDeadline(LocalDate.now().plusDays(daysOut));
        return s;
    }

    @Test
    void run_emitsOneEventAndMarksEachDueScholarship() {
        when(mapper.findDueForDeadlineReminder(10)).thenReturn(List.of(due(1L, 8), due(2L, 3)));

        job.run();

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(outbox, times(2)).write(eq("scholarship"), anyLong(),
                eq(OutboxEventTypes.SCHOLARSHIP_DEADLINE_REMINDER), payload.capture());
        verify(mapper).insertDeadlineReminder(1L);
        verify(mapper).insertDeadlineReminder(2L);

        assertThat(payload.getAllValues().get(0))
                .contains("\"scholarshipTitle\":\"CSC Master Scholarship\"")
                .contains("\"daysLeft\":8");
    }

    @Test
    void run_doesNothingWhenNoScholarshipIsDue() {
        when(mapper.findDueForDeadlineReminder(10)).thenReturn(List.of());

        job.run();

        verify(outbox, never()).write(anyString(), anyLong(), anyString(), anyString());
        verify(mapper, never()).insertDeadlineReminder(anyLong());
    }
}
