package com.nadoumi.application.service;

import com.alibaba.fastjson2.JSONObject;
import com.nadoumi.application.domain.Application;
import com.nadoumi.application.domain.ApplicationEvent;
import com.nadoumi.application.domain.ApplicationInterview;
import com.nadoumi.application.domain.ApplicationTask;
import com.nadoumi.application.domain.enums.ApplicationTaskStatus;
import com.nadoumi.application.mapper.ApplicationEventMapper;
import com.nadoumi.application.mapper.ApplicationInterviewMapper;
import com.nadoumi.application.mapper.ApplicationMapper;
import com.nadoumi.application.mapper.ApplicationTaskMapper;
import com.nadoumi.application.web.request.ScheduleInterviewRequest;
import com.nadoumi.application.web.response.InterviewResponse;
import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.exception.NadNotFoundException;
import com.ruoyi.common.utils.AuditActor;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Interviews the university or Nadoumi asks of an applicant. Staff schedule one while the case is in "Interview
 * required"; the time is entered in Beijing time but stored as an instant in UTC, and the student is told in both
 * Beijing time and UTC (their dashboard shows it in their own zone). Rescheduling cancels the open interview first, so
 * there is never more than one live interview and the earlier ones stay as history.
 */
@Service
public class ApplicationInterviewService {

    static final String STATUS_INTERVIEW_REQUIRED = "INTERVIEW_REQUIRED";
    /** The title of the seeded checklist item that scheduling completes (workflow V140). */
    static final String SCHEDULE_TASK_TITLE = "Schedule the interview and send the details to the student";

    private static final ZoneId BEIJING = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter BEIJING_FORMAT =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'at' HH:mm", Locale.ENGLISH).withZone(BEIJING);
    private static final DateTimeFormatter UTC_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH).withZone(ZoneOffset.UTC);

    private final ApplicationInterviewMapper interviews;
    private final ApplicationMapper applications;
    private final ApplicationTaskMapper tasks;
    private final ApplicationEventMapper events;
    private final ApplicationOutboxEmitter emitter;
    private final Clock clock;

    @Autowired
    public ApplicationInterviewService(ApplicationInterviewMapper interviews, ApplicationMapper applications,
            ApplicationTaskMapper tasks, ApplicationEventMapper events, ApplicationOutboxEmitter emitter) {
        this(interviews, applications, tasks, events, emitter, Clock.systemUTC());
    }

    ApplicationInterviewService(ApplicationInterviewMapper interviews, ApplicationMapper applications,
            ApplicationTaskMapper tasks, ApplicationEventMapper events, ApplicationOutboxEmitter emitter, Clock clock) {
        this.interviews = interviews;
        this.applications = applications;
        this.tasks = tasks;
        this.events = events;
        this.emitter = emitter;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<InterviewResponse> list(long applicationId) {
        load(applicationId);
        return interviews.findByApplication(applicationId).stream().map(InterviewResponse::of).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public InterviewResponse schedule(long applicationId, ScheduleInterviewRequest req, long actorUserId) {
        Application application = load(applicationId);
        if (!STATUS_INTERVIEW_REQUIRED.equals(application.getCurrentStatus())) {
            throw new NadBadRequestException("an interview can only be scheduled while the status is Interview required");
        }
        LocalDateTime startsAt = req.startsAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        if (!startsAt.isAfter(LocalDateTime.now(clock))) {
            throw new NadBadRequestException("the interview must be in the future");
        }
        String url = requireHttpsUrl(req.meetingUrl());

        ApplicationInterview previous = interviews.findScheduled(applicationId);
        if (previous != null) {
            interviews.cancel(previous.getId(), AuditActor.username());
        }
        ApplicationInterview row = new ApplicationInterview();
        row.setApplicationId(applicationId);
        row.setStartsAt(startsAt);
        row.setDurationMinutes(req.durationMinutes());
        row.setPlatform(blankToNull(req.platform()));
        row.setMeetingUrl(url);
        row.setInstructions(blankToNull(req.instructions()));
        row.setCreateBy(AuditActor.username());
        try {
            interviews.insert(row);
        }
        catch (DuplicateKeyException raced) {
            throw new NadBadRequestException("another interview was scheduled at the same moment; reload and try again");
        }
        completeScheduleTask(applicationId);
        appendEvent(applicationId, previous == null ? "INTERVIEW_SCHEDULED" : "INTERVIEW_RESCHEDULED", actorUserId,
                beijingTime(row));
        emitter.update(application, previous == null ? "Your interview is scheduled" : "Your interview was rescheduled",
                message(row));
        return InterviewResponse.of(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(long applicationId, long interviewId, long actorUserId) {
        Application application = load(applicationId);
        ApplicationInterview row = interviews.findById(interviewId);
        if (row == null || !row.getApplicationId().equals(applicationId)) {
            throw new NadNotFoundException("interview not found on this application");
        }
        if (interviews.cancel(interviewId, AuditActor.username()) == 0) {
            return; // already cancelled: nothing to undo and nobody to tell twice
        }
        appendEvent(applicationId, "INTERVIEW_CANCELLED", actorUserId, beijingTime(row));
        emitter.update(application, "Your interview was cancelled",
                "The interview planned for " + beijingTime(row) + " was cancelled. Nadoumi will send you the new details.");
    }

    /** The message the student receives: Beijing time first (it is the interview's own clock), then UTC, link and notes. */
    static String message(ApplicationInterview i) {
        StringBuilder text = new StringBuilder("Your interview is on ").append(beijingTime(i)).append(" Beijing time (UTC+8), which is ")
                .append(UTC_FORMAT.format(i.getStartsAt().toInstant(ZoneOffset.UTC))).append(" UTC. It lasts ")
                .append(i.getDurationMinutes()).append(" minutes.");
        text.append("\n\nJoin here: ").append(i.getMeetingUrl());
        if (i.getPlatform() != null) {
            text.append(" (").append(i.getPlatform()).append(')');
        }
        if (i.getInstructions() != null) {
            text.append("\n\n").append(i.getInstructions());
        }
        text.append("\n\nYour dashboard shows the time in your own time zone.");
        return text.toString();
    }

    private static String beijingTime(ApplicationInterview i) {
        return BEIJING_FORMAT.format(i.getStartsAt().toInstant(ZoneOffset.UTC));
    }

    private void completeScheduleTask(long applicationId) {
        for (ApplicationTask task : tasks.findByApplication(applicationId)) {
            if (SCHEDULE_TASK_TITLE.equals(task.getTitle()) && task.getStatus() == ApplicationTaskStatus.OPEN) {
                tasks.updateStatus(task.getId(), ApplicationTaskStatus.DONE.name(), null, AuditActor.username());
            }
        }
    }

    private void appendEvent(long applicationId, String type, long actorUserId, String detail) {
        ApplicationEvent event = new ApplicationEvent();
        event.setApplicationId(applicationId);
        event.setEventType(type);
        event.setActorUserId(actorUserId);
        event.setDetailJson(JSONObject.of("note", detail).toJSONString());
        events.insert(event);
    }

    private Application load(long applicationId) {
        Application application = applications.findById(applicationId);
        if (application == null) {
            throw new NadNotFoundException("application not found");
        }
        return application;
    }

    /** A meeting link is shown to a student as a link, so only https is accepted (no javascript:, no plain http). */
    private static String requireHttpsUrl(String raw) {
        String url = raw.trim();
        try {
            URI uri = URI.create(url);
            if ("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null) {
                return url;
            }
        }
        catch (IllegalArgumentException ignored) {
            // falls through to the same message as a non-https link
        }
        throw new NadBadRequestException("the meeting link must be a valid https link");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
