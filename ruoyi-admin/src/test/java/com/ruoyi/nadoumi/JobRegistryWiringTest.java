package com.ruoyi.nadoumi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ruoyi.quartz.util.JobRegistry;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * Against the real Spring context: the platform's scheduled beans are registered,
 * every job a migration seeds is runnable, and the job-create endpoint refuses
 * targets that are not {@code <registeredBean>.run()}.
 */
class JobRegistryWiringTest extends AbstractNadIntegrationTest {

    private static final List<String> PLATFORM_JOBS = List.of(
            "outboxPollerJob.run()",
            "notificationDispatchJob.run()",
            "mediaReconciliationJob.run()",
            "scholarshipDeadlineReminderJob.run()");

    @Autowired
    private JobRegistry registry;

    @Test
    void shouldRegisterEveryPlatformJob() {
        PLATFORM_JOBS.forEach(target -> assertThat(registry.isAllowed(target)).as(target).isTrue());
    }

    @Test
    void shouldAllowEverySeededNadoumiJobTarget() {
        // Baseline RuoYi demo rows (ryTask.*) point at a bean that no longer exists and are seeded paused.
        List<String> seeded = jdbc.queryForList(
                "select invoke_target from sys_job where invoke_target not like 'ryTask.%'", String.class);

        assertThat(seeded).isNotEmpty();
        seeded.forEach(target -> assertThat(registry.isAllowed(target)).as(target).isTrue());
    }

    @Test
    void shouldRefuseJobCreation_whenTargetIsNotARegisteredBean() throws Exception {
        long roleId = jdbc.queryForObject("select role_id from sys_role where role_key = 'ops_manager'", Long.class);
        long menuId = jdbc.queryForObject("select menu_id from sys_menu where perms = 'monitor:job:add'", Long.class);
        jdbc.update("insert into sys_role_menu (role_id, menu_id) values (?, ?)", roleId, menuId);
        try {
            createStaff("job_admin", "ops_manager");
            String token = staffToken("job_admin");

            for (String target : List.of(
                    "com.nadoumi.notification.job.OutboxPollerJob.run()",
                    "outboxPollerJob.toString()",
                    "outboxPollerJob.run('x')",
                    "studentAuthService.login()")) {
                mvc.perform(post("/monitor/job")
                                .header("Authorization", bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jobBody(target)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(500))
                        .andExpect(jsonPath("$.msg").value(Matchers.containsString("registeredJobBean")));
            }
            assertThat(jdbc.queryForObject("select count(*) from sys_job where job_name = 'probe'", Integer.class))
                    .isZero();
        }
        finally {
            jdbc.update("delete from sys_role_menu where role_id = ? and menu_id = ?", roleId, menuId);
        }
    }

    private static String jobBody(String invokeTarget) {
        return "{\"jobName\":\"probe\",\"jobGroup\":\"DEFAULT\",\"invokeTarget\":\"" + invokeTarget
                + "\",\"cronExpression\":\"0 0 0 * * ?\",\"misfirePolicy\":\"3\","
                + "\"concurrent\":\"1\",\"status\":\"1\"}";
    }
}
