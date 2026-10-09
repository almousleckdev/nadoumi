package com.ruoyi.nadoumi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Deleting a person from the admin deletes them: the same student can neither sign in nor hold the username
 * afterwards, a deleted staff account is gone completely, and a student who never finished onboarding can be
 * found and deleted too.
 */
class AccountDeletionTest extends AbstractStudentIntegrationTest {

    private void registerAs(String username, String email, String password) throws Exception {
        String ticket = ticketFor(email);
        mvc.perform(post("/api/student/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"email\":\"" + email + "\",\"password\":\""
                                + password + "\",\"ticket\":\"" + ticket + "\"}"))
                .andExpect(status().isCreated());
    }

    private long applicantOf(String email) {
        return jdbc.queryForObject(
                "select a.id from nad_applicant a join sys_user u on u.user_id = a.create_by where u.email = ?", Long.class, email);
    }

    private boolean canSignIn(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/student/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return body.contains("\"token\"");
    }

    @Test
    void shouldNotLetADeletedStudentSignInOrKeepTheirUsername_evenWhenTheyNeverFinishedOnboarding() throws Exception {
        String email = uniqueEmail();
        registerAs("mohamed", email, STRONG_PASSWORD);
        assertThat(canSignIn(email, STRONG_PASSWORD)).isTrue();
        createStaff("acc_admin", "nadoumi_super_admin");
        String staff = bearer(staffToken("acc_admin"));

        mvc.perform(delete("/api/staff/applicants/" + pid(applicantOf(email))).header("Authorization", staff))
                .andExpect(status().isNoContent());

        assertThat(canSignIn(email, STRONG_PASSWORD)).as("the deleted student can still sign in").isFalse();
        mvc.perform(get("/api/student/username-available").param("username", "mohamed"))
                .andExpect(jsonPath("$.available").value(true));
        assertThat(jdbc.queryForObject("select count(*) from sys_user where user_name = 'mohamed' or email = ?", Integer.class, email))
                .as("no trace of the account is left").isZero();
        registerAs("mohamed", uniqueEmail(), STRONG_PASSWORD);
    }

    @Test
    void shouldTellStaffHowManySignUpsTheDefaultListHides_soTheyCanBeOpenedAndDeleted() throws Exception {
        registerAs("hidden_one", uniqueEmail(), STRONG_PASSWORD);
        createStaff("acc_admin2", "nadoumi_super_admin");
        String staff = bearer(staffToken("acc_admin2"));

        mvc.perform(get("/api/staff/applicants/incomplete-count").header("Authorization", staff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
        String list = mvc.perform(get("/api/staff/applicants?incomplete=true").header("Authorization", staff))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String publicId = JsonPath.<java.util.List<String>>read(list, "$.content[*].publicId").get(0);

        mvc.perform(delete("/api/staff/applicants/" + publicId).header("Authorization", staff)).andExpect(status().isNoContent());

        mvc.perform(get("/api/staff/applicants/incomplete-count").header("Authorization", staff))
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void shouldDeleteAStaffAccountCompletely_includingTheirChats() throws Exception {
        createStaff("acc_boss", "nadoumi_super_admin");
        long victim = createStaff("acc_leaver", "ops_manager");
        Student s = register("Chat", "Mate");
        long studentUser = jdbc.queryForObject("select user_id from sys_user where email = ?", Long.class, s.email());
        mvc.perform(post("/api/staff/conversations/direct").header("Authorization", bearer(staffToken("acc_leaver")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":" + studentUser + "}"))
                .andExpect(status().isOk());

        mvc.perform(delete("/system/user/" + victim).header("Authorization", bearer(staffToken("acc_boss"))))
                .andExpect(jsonPath("$.code").value(200));

        assertThat(jdbc.queryForObject("select count(*) from sys_user where user_id = ?", Integer.class, victim)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from sys_user where user_name = 'acc_leaver'", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from nad_conversation", Integer.class)).isZero();
    }

    @Test
    void shouldRefuseToDeleteAStudentFromTheStaffScreen_andNeverTouchTheirAccount() throws Exception {
        createStaff("acc_boss2", "nadoumi_super_admin");
        Student s = register("Keep", "Safe");
        long studentUser = jdbc.queryForObject("select user_id from sys_user where email = ?", Long.class, s.email());

        mvc.perform(delete("/system/user/" + studentUser).header("Authorization", bearer(staffToken("acc_boss2"))))
                .andExpect(jsonPath("$.code").value(500));

        assertThat(jdbc.queryForObject("select count(*) from sys_user where user_id = ?", Integer.class, studentUser)).isEqualTo(1);
    }

    @Test
    void shouldNotLetStaffDeleteTheirOwnAccount() throws Exception {
        long me = createStaff("acc_self", "nadoumi_super_admin");

        mvc.perform(delete("/system/user/" + me).header("Authorization", bearer(staffToken("acc_self"))))
                .andExpect(jsonPath("$.code").value(500));

        assertThat(jdbc.queryForObject("select count(*) from sys_user where user_id = ?", Integer.class, me)).isEqualTo(1);
    }
}
