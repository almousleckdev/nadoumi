package com.ruoyi.nadoumi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Staff deletion of a student account: who may do it, and what it leaves behind. */
class StudentDeletionTest extends AbstractStudentIntegrationTest {

    private long userIdOf(Student s) {
        return jdbc.queryForObject("select user_id from sys_user where email = ?", Long.class, s.email());
    }

    @Test
    void shouldDeleteTheAccountEndItsSessionAndArchiveTheApplicant_whenSuperAdminDeletes() throws Exception {
        Student s = register("Dele", "Ted");
        long userId = userIdOf(s);
        createStaff("del_admin", "nadoumi_super_admin");

        mvc.perform(delete("/api/staff/students/" + userId).header("Authorization", bearer(staffToken("del_admin"))))
                .andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("select del_flag from sys_user where user_id = ?", String.class, userId))
                .isEqualTo("2");
        assertThat(jdbc.queryForObject("select status from nad_applicant where id = ?", String.class, s.applicantId()))
                .isEqualTo("ARCHIVED");
        assertThat(jdbc.queryForObject(
                "select count(*) from nad_user_applicant_access where user_id = ? and status = 'ACTIVE'",
                Integer.class, userId)).isZero();
        mvc.perform(get("/api/student/applicants").header("Authorization", bearer(s.token())))
                .andExpect(jsonPath("$.code").value(401)); // RuoYi renders a rejected call as 200 with the code in the body
        mvc.perform(post("/api/student/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + s.email() + "\",\"password\":\"" + STRONG_PASSWORD + "\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void shouldForbidDeleting_whenStaffLacksTheDeletePermission() throws Exception {
        Student s = register("Keep", "Me");
        createStaff("del_ops", "ops_manager");

        mvc.perform(delete("/api/staff/students/" + userIdOf(s)).header("Authorization", bearer(staffToken("del_ops"))))
                .andExpect(status().isForbidden());

        assertThat(jdbc.queryForObject("select del_flag from sys_user where user_id = ?", String.class, userIdOf(s)))
                .isEqualTo("0");
    }

    @Test
    void shouldForbidDeleting_whenTheCallerIsAStudent() throws Exception {
        Student s = register("Sneaky", "One");
        Student other = register("Vic", "Tim");

        mvc.perform(delete("/api/staff/students/" + userIdOf(other)).header("Authorization", bearer(s.token())))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldFreeTheEmailForANewRegistration_afterDeletion() throws Exception {
        Student s = register("Again", "Soon");
        long userId = userIdOf(s);
        createStaff("del_admin2", "nadoumi_super_admin");

        mvc.perform(delete("/api/staff/students/" + userId).header("Authorization", bearer(staffToken("del_admin2"))))
                .andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("select count(*) from sys_user where email = ? and del_flag = '0'",
                Integer.class, s.email())).isZero();
        assertThat(jdbc.queryForObject("select email from sys_user where user_id = ?", String.class, userId))
                .isEmpty();
    }
}
