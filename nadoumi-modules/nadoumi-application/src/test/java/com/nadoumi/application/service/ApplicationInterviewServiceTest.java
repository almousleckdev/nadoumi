package com.nadoumi.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.application.domain.Application;
import com.nadoumi.application.domain.ApplicationInterview;
import com.nadoumi.application.domain.ApplicationTask;
import com.nadoumi.application.domain.enums.ApplicationTaskStatus;
import com.nadoumi.application.mapper.ApplicationEventMapper;
import com.nadoumi.application.mapper.ApplicationInterviewMapper;
import com.nadoumi.application.mapper.ApplicationMapper;
import com.nadoumi.application.mapper.ApplicationTaskMapper;
import com.nadoumi.application.web.request.ScheduleInterviewRequest;
import com.nadoumi.common.exception.NadBadRequestException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

class ApplicationInterviewServiceTest {

    private static final long APPLICATION_ID = 5L;
    private static final long STAFF_ID = 3L;
    private static final Instant NOW = Instant.parse("2026-10-26T00:00:00Z");

    private final ApplicationInterviewMapper interviews = mock(ApplicationInterviewMapper.class);
    private final ApplicationMapper applications = mock(ApplicationMapper.class);
    private final ApplicationTaskMapper tasks = mock(ApplicationTaskMapper.class);
    private final ApplicationEventMapper events = mock(ApplicationEventMapper.class);
    private final ApplicationOutboxEmitter emitter = mock(ApplicationOutboxEmitter.class);
    private final ApplicationInterviewService service = new ApplicationInterviewService(interviews, applications, tasks,
            events, emitter, Clock.fixed(NOW, ZoneOffset.UTC));

    private Application application(String status) {
        Application a = new Application();
        a.setId(APPLICATION_ID);
        a.setCurrentStatus(status);
        when(applications.findById(APPLICATION_ID)).thenReturn(a);
        return a;
    }

    private static ScheduleInterviewRequest request(String startsAt, String url) {
        return new ScheduleInterviewRequest(OffsetDateTime.parse(startsAt), 30, "Zoom", url, "Bring your passport.");
    }

    @Test
    void shouldStoreTheBeijingTimeAsUtc_andTellTheStudentInBothZones() {
        Application a = application("INTERVIEW_REQUIRED");

        service.schedule(APPLICATION_ID, request("2026-11-02T15:00:00+08:00", "https://meet.example/abc"), STAFF_ID);

        var saved = org.mockito.ArgumentCaptor.forClass(ApplicationInterview.class);
        verify(interviews).insert(saved.capture());
        assertThat(saved.getValue().getStartsAt()).isEqualTo(LocalDateTime.parse("2026-11-02T07:00:00"));
        verify(emitter).update(eq(a), eq("Your interview is scheduled"),
                org.mockito.ArgumentMatchers.argThat(m -> m.contains("Monday 2 November 2026 at 15:00 Beijing time")
                        && m.contains("07:00 UTC") && m.contains("https://meet.example/abc") && m.contains("Bring your passport.")));
    }

    @Test
    void shouldCancelTheOpenInterview_whenRescheduling() {
        application("INTERVIEW_REQUIRED");
        ApplicationInterview open = new ApplicationInterview();
        open.setId(9L);
        when(interviews.findScheduled(APPLICATION_ID)).thenReturn(open);

        service.schedule(APPLICATION_ID, request("2026-11-03T10:00:00+08:00", "https://meet.example/abc"), STAFF_ID);

        verify(interviews).cancel(eq(9L), any());
        verify(emitter).update(any(), eq("Your interview was rescheduled"), any());
    }

    @Test
    void shouldCompleteTheScheduleTask() {
        application("INTERVIEW_REQUIRED");
        ApplicationTask task = new ApplicationTask();
        task.setId(44L);
        task.setTitle(ApplicationInterviewService.SCHEDULE_TASK_TITLE);
        task.setStatus(ApplicationTaskStatus.OPEN);
        when(tasks.findByApplication(APPLICATION_ID)).thenReturn(List.of(task));

        service.schedule(APPLICATION_ID, request("2026-11-02T15:00:00+08:00", "https://meet.example/abc"), STAFF_ID);

        verify(tasks).updateStatus(eq(44L), eq("DONE"), eq(null), any());
    }

    @Test
    void shouldRefuse_whenTheCaseIsNotWaitingForAnInterview() {
        application("UNDER_UNIVERSITY_REVIEW");

        assertThatThrownBy(() -> service.schedule(APPLICATION_ID,
                request("2026-11-02T15:00:00+08:00", "https://meet.example/abc"), STAFF_ID))
                .isInstanceOf(NadBadRequestException.class);
        verify(interviews, never()).insert(any());
    }

    @Test
    void shouldRefuseAPastTime_andALinkThatIsNotHttps() {
        application("INTERVIEW_REQUIRED");

        assertThatThrownBy(() -> service.schedule(APPLICATION_ID,
                request("2026-10-25T10:00:00+08:00", "https://meet.example/abc"), STAFF_ID))
                .isInstanceOf(NadBadRequestException.class).hasMessageContaining("future");
        assertThatThrownBy(() -> service.schedule(APPLICATION_ID,
                request("2026-11-02T15:00:00+08:00", "javascript:alert(1)"), STAFF_ID))
                .isInstanceOf(NadBadRequestException.class).hasMessageContaining("https");
        assertThatThrownBy(() -> service.schedule(APPLICATION_ID,
                request("2026-11-02T15:00:00+08:00", "http://meet.example/abc"), STAFF_ID))
                .isInstanceOf(NadBadRequestException.class);
        verify(interviews, never()).insert(any());
    }

    @Test
    void shouldReportARace_asABadRequest() {
        application("INTERVIEW_REQUIRED");
        doThrow(new DuplicateKeyException("uk_interview_one_scheduled")).when(interviews).insert(any());

        assertThatThrownBy(() -> service.schedule(APPLICATION_ID,
                request("2026-11-02T15:00:00+08:00", "https://meet.example/abc"), STAFF_ID))
                .isInstanceOf(NadBadRequestException.class);
        verify(emitter, never()).update(any(), any(), any());
    }

    @Test
    void shouldNotTellTheStudentTwice_whenAnInterviewIsCancelledTwice() {
        application("INTERVIEW_REQUIRED");
        ApplicationInterview row = new ApplicationInterview();
        row.setId(9L);
        row.setApplicationId(APPLICATION_ID);
        row.setStartsAt(LocalDateTime.parse("2026-11-02T07:00:00"));
        when(interviews.findById(9L)).thenReturn(row);
        when(interviews.cancel(eq(9L), any())).thenReturn(1, 0);

        service.cancel(APPLICATION_ID, 9L, STAFF_ID);
        service.cancel(APPLICATION_ID, 9L, STAFF_ID);

        verify(emitter, org.mockito.Mockito.times(1)).update(any(), eq("Your interview was cancelled"), contains("Monday 2 November"));
        verify(events, org.mockito.Mockito.times(1)).insert(any());
    }
}
