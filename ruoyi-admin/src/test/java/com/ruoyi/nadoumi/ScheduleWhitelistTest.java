package com.ruoyi.nadoumi;

import static org.assertj.core.api.Assertions.assertThat;

import com.ruoyi.quartz.util.ScheduleUtils;
import org.junit.jupiter.api.Test;

/**
 * {@link ScheduleUtils#whiteList} must accept the Nadoumi Quartz-job packages
 * (so those jobs are manageable from the admin console) and still reject an
 * arbitrary fully-qualified {@code invokeTarget}. Only the FQCN branch is
 * exercised here — the bean-name branch needs a Spring context.
 */
class ScheduleWhitelistTest {

    @Test
    void acceptsNadoumiJobPackages() {
        assertThat(ScheduleUtils.whiteList("com.nadoumi.notification.job.OutboxPollerJob.run()")).isTrue();
        assertThat(ScheduleUtils.whiteList("com.nadoumi.scholarship.job.ScholarshipDeadlineReminderJob.run()")).isTrue();
        assertThat(ScheduleUtils.whiteList("com.nadoumi.media.job.MediaReconciliationJob.run()")).isTrue();
        assertThat(ScheduleUtils.whiteList("com.ruoyi.quartz.task.RyTask.ryParams('x')")).isTrue();
    }

    @Test
    void rejectsArbitraryTargets() {
        assertThat(ScheduleUtils.whiteList("java.lang.Runtime.getRuntime()")).isFalse();
        assertThat(ScheduleUtils.whiteList("com.nadoumi.identity.service.StudentAuthService.login()")).isFalse();
        assertThat(ScheduleUtils.whiteList("org.springframework.context.ApplicationContext.getBean('x')")).isFalse();
    }
}
