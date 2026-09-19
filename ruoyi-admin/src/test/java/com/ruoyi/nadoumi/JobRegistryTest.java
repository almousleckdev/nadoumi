package com.ruoyi.nadoumi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ruoyi.common.core.job.SchedulableJob;
import com.ruoyi.quartz.util.JobRegistry;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * The scheduler may only run registered {@link SchedulableJob} beans through
 * {@code <beanName>.run()}. Anything else a {@code sys_job} row (or an admin
 * with the job-edit permission) can type must be refused.
 */
class JobRegistryTest {

    private final AtomicInteger runs = new AtomicInteger();
    private final JobRegistry registry = new JobRegistry(Map.of("outboxPollerJob", runs::incrementAndGet));

    @Test
    void shouldRunRegisteredJob_whenTargetIsBeanNameDotRun() {
        registry.run("outboxPollerJob.run()");

        assertThat(runs).hasValue(1);
    }

    @Test
    void shouldAllowTarget_whenBeanIsRegistered() {
        assertThat(registry.isAllowed("outboxPollerJob.run()")).isTrue();
    }

    @Test
    void shouldRejectTarget_whenBeanIsNotRegistered() {
        assertThat(registry.isAllowed("studentAuthService.run()")).isFalse();
        assertThatThrownBy(() -> registry.run("studentAuthService.run()"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(runs).hasValue(0);
    }

    @Test
    void shouldRejectTarget_whenItNamesAClassInsteadOfABean() {
        assertThat(registry.isAllowed("com.nadoumi.notification.job.OutboxPollerJob.run()")).isFalse();
        assertThat(registry.isAllowed("java.lang.Runtime.getRuntime()")).isFalse();
    }

    @Test
    void shouldRejectTarget_whenMethodIsNotRun() {
        assertThat(registry.isAllowed("outboxPollerJob.toString()")).isFalse();
        assertThat(registry.isAllowed("outboxPollerJob.hashCode()")).isFalse();
    }

    @Test
    void shouldRejectTarget_whenItCarriesArgumentsOrTrailingText() {
        assertThat(registry.isAllowed("outboxPollerJob.run('x')")).isFalse();
        assertThat(registry.isAllowed("outboxPollerJob.run();other.run()")).isFalse();
        assertThat(registry.isAllowed("outboxPollerJob.run() ")).isFalse();
        assertThat(registry.isAllowed("outboxPollerJob.run")).isFalse();
    }

    @Test
    void shouldRejectTarget_whenNullOrBlank() {
        assertThat(registry.isAllowed(null)).isFalse();
        assertThat(registry.isAllowed("")).isFalse();
    }
}
