package com.nadoumi.scholarship.job;

import com.alibaba.fastjson2.JSONObject;
import com.nadoumi.common.outbox.OutboxEventTypes;
import com.nadoumi.common.outbox.OutboxWriter;
import com.nadoumi.scholarship.domain.Scholarship;
import com.nadoumi.scholarship.mapper.ScholarshipMapper;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Daily Quartz job (bean name {@code scholarshipDeadlineReminderJob}; seeded
 * active by V53 as {@code scholarshipDeadlineReminderJob.run()}). For every
 * published + active scholarship whose deadline is within
 * {@link #REMINDER_WINDOW_DAYS} days and has not been reminded yet, it writes one
 * {@code ScholarshipDeadlineReminder} outbox event and records the reminder so it
 * fires exactly once. {@code OutboxToNotificationDispatcher} fans that event to
 * every active registered student — IN_APP always, EMAIL by preference.
 */
@Component("scholarshipDeadlineReminderJob")
public class ScholarshipDeadlineReminderJob {

    private static final Logger log = LoggerFactory.getLogger(ScholarshipDeadlineReminderJob.class);

    static final int REMINDER_WINDOW_DAYS = 10;

    private final ScholarshipMapper mapper;
    private final OutboxWriter outbox;

    public ScholarshipDeadlineReminderJob(ScholarshipMapper mapper, OutboxWriter outbox) {
        this.mapper = mapper;
        this.outbox = outbox;
    }

    @Transactional(rollbackFor = Exception.class)
    public void run() {
        List<Scholarship> due = mapper.findDueForDeadlineReminder(REMINDER_WINDOW_DAYS);
        if (due.isEmpty()) {
            return;
        }
        LocalDate today = LocalDate.now();
        for (Scholarship s : due) {
            long daysLeft = ChronoUnit.DAYS.between(today, s.getDeadline());

            JSONObject payload = new JSONObject();
            payload.put("scholarshipId", s.getId());
            payload.put("scholarshipTitle", s.getTitle());
            payload.put("scholarshipSlug", s.getSlug() == null ? "" : s.getSlug());
            payload.put("scholarshipReference", s.getReferenceCode() == null ? "" : s.getReferenceCode());
            payload.put("deadline", s.getDeadline().toString());
            payload.put("daysLeft", daysLeft);

            outbox.write("scholarship", s.getId(),
                    OutboxEventTypes.SCHOLARSHIP_DEADLINE_REMINDER, payload.toJSONString());
            mapper.insertDeadlineReminder(s.getId());
        }
        log.info("scholarship deadline reminders queued: {}", due.size());
    }
}
