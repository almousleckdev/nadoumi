package com.ruoyi.nadoumi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Suspending or blocking an account (student or staff) must carry a written reason. */
class UserStatusReasonTest extends AbstractNadIntegrationTest {

    private ResultActions changeStatus(String adminToken, long userId, String status, String reason) throws Exception {
        String body = "{\"userId\":" + userId + ",\"status\":\"" + status + "\""
                + (reason == null ? "" : ",\"statusReason\":\"" + reason + "\"") + "}";
        return mvc.perform(put("/system/user/changeStatus").header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String reasonOf(long userId) {
        return jdbc.queryForObject("select status_reason from sys_user where user_id = ?", String.class, userId);
    }

    private String statusOf(long userId) {
        return jdbc.queryForObject("select status from sys_user where user_id = ?", String.class, userId);
    }

    @Test
    void shouldRefuseToBlockWithoutAReason_forAStudent() throws Exception {
        createStaff("rsn_admin", "nadoumi_super_admin");
        long student = createStudent("rsn_student");

        changeStatus(staffToken("rsn_admin"), student, "2", null).andExpect(jsonPath("$.code").value(500));
        changeStatus(staffToken("rsn_admin"), student, "2", "no").andExpect(jsonPath("$.code").value(500));

        assertThat(statusOf(student)).isEqualTo("0");
    }

    @Test
    void shouldRefuseToSuspendWithoutAReason_forStaff() throws Exception {
        createStaff("rsn_admin2", "nadoumi_super_admin");
        long other = createStaff("rsn_other", "ops_manager");

        changeStatus(staffToken("rsn_admin2"), other, "1", "   ").andExpect(jsonPath("$.code").value(500));

        assertThat(statusOf(other)).isEqualTo("0");
    }

    @Test
    void shouldStoreTheReasonAndClearItOnActivation() throws Exception {
        createStaff("rsn_admin3", "nadoumi_super_admin");
        long student = createStudent("rsn_student3");
        String admin = staffToken("rsn_admin3");

        changeStatus(admin, student, "2", "Fake passport submitted").andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        assertThat(statusOf(student)).isEqualTo("2");
        assertThat(reasonOf(student)).isEqualTo("Fake passport submitted");

        changeStatus(admin, student, "0", "ignored on activation").andExpect(jsonPath("$.code").value(200));
        assertThat(statusOf(student)).isEqualTo("0");
        assertThat(reasonOf(student)).isNull();
    }
}
