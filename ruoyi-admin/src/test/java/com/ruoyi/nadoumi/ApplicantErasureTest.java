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

/** Permanent deletion of an applicant: it must work for every applicant, whatever hangs off it. */
class ApplicantErasureTest extends AbstractStudentIntegrationTest {

    private long userIdOf(Student s) {
        return jdbc.queryForObject("select user_id from sys_user where email = ?", Long.class, s.email());
    }

    private long count(String table, String column, long id) {
        return jdbc.queryForObject("select count(*) from " + table + " where " + column + " = ?", Long.class, id);
    }

    /** A student with a filled profile, photo, passport, a document and an application: the hardest case. */
    private Student fullStudent(String first, String last) throws Exception {
        Student s = register(first, last);
        fillProfileSections(s);
        upload(s, "/photo", "me.png").andExpect(status().isCreated());
        String scan = upload(s, "/passport/scan", "passport.png").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long mediaId = ((Number) JsonPath.read(scan, "$.mediaId")).longValue();

        jdbc.update("insert into nad_document (applicant_id, doc_type) values (?, 'PASSPORT')", s.applicantId());
        long documentId = jdbc.queryForObject("select max(id) from nad_document", Long.class);
        jdbc.update("insert into nad_document_version (document_id, version_no, media_asset_id, content_type, size_bytes,"
                + " uploaded_by, uploaded_at) values (?, 1, ?, 'image/png', 10, ?, now())", documentId, mediaId, userIdOf(s));
        jdbc.update("insert into nad_university (name, slug, country) values ('Erasure U', 'erasure-u', 'CN')");
        long universityId = jdbc.queryForObject("select max(id) from nad_university", Long.class);
        jdbc.update("insert into nad_program (university_id, name, slug, program_type) values (?, 'Erasure MBA', 'erasure-mba', 'MASTER')",
                universityId);
        long programId = jdbc.queryForObject("select max(id) from nad_program", Long.class);
        jdbc.update("insert into nad_application (applicant_id, application_type, program_id) values (?, 'PROGRAM', ?)",
                s.applicantId(), programId);
        return s;
    }

    @Test
    void shouldEraseEverythingAndCloseTheLogin_whenSuperAdminDeletesAFullyPopulatedApplicant() throws Exception {
        Student s = fullStudent("Dele", "Ted");
        long userId = userIdOf(s);
        long applicant = s.applicantId();
        createStaff("era_admin", "nadoumi_super_admin");

        mvc.perform(delete("/api/staff/applicants/" + pid(applicant)).header("Authorization", bearer(staffToken("era_admin"))))
                .andExpect(status().isNoContent());

        for (String table : new String[] {"nad_applicant_education", "nad_applicant_contact", "nad_applicant_residence",
                "nad_applicant_interest", "nad_application", "nad_document", "nad_user_applicant_access"}) {
            assertThat(count(table, "applicant_id", applicant)).as(table).isZero();
        }
        assertThat(count("nad_applicant", "id", applicant)).isZero();
        assertThat(count("sys_user", "user_id", userId)).as("the account row is gone, not kept").isZero();
        mvc.perform(get("/api/student/applicants").header("Authorization", bearer(s.token())))
                .andExpect(jsonPath("$.code").value(401)); // RuoYi renders a rejected call as 200 with the code in the body
        mvc.perform(post("/api/student/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + s.email() + "\",\"password\":\"" + STRONG_PASSWORD + "\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void shouldDeleteAnApplicantThatHasNoStudentAccount() throws Exception {
        createStaff("era_admin2", "nadoumi_super_admin");
        String token = bearer(staffToken("era_admin2"));
        String created = mvc.perform(post("/api/staff/applicants").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"givenName\":\"Walk\",\"familyName\":\"In\",\"nationality\":\"MR\",\"invitedEmail\":\"walkin@example.test\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(created, "$.id")).longValue();

        mvc.perform(delete("/api/staff/applicants/" + pid(id)).header("Authorization", token)).andExpect(status().isNoContent());

        assertThat(count("nad_applicant", "id", id)).isZero();
    }

    @Test
    void shouldFreeTheEmailForANewRegistration_afterDeletion() throws Exception {
        Student s = register("Again", "Soon");
        long userId = userIdOf(s);
        createStaff("era_admin3", "nadoumi_super_admin");

        mvc.perform(delete("/api/staff/applicants/" + pid(s.applicantId())).header("Authorization", bearer(staffToken("era_admin3"))))
                .andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("select count(*) from sys_user where email = ? and del_flag = '0'", Integer.class, s.email()))
                .isZero();
        assertThat(count("sys_user", "user_id", userId)).isZero();
    }

    @Test
    void shouldForbidDeleting_whenStaffLacksThePermission() throws Exception {
        Student s = register("Keep", "Me");
        createStaff("era_ops", "ops_manager");

        mvc.perform(delete("/api/staff/applicants/" + pid(s.applicantId())).header("Authorization", bearer(staffToken("era_ops"))))
                .andExpect(status().isForbidden());

        assertThat(count("nad_applicant", "id", s.applicantId())).isEqualTo(1);
    }

    @Test
    void shouldForbidDeleting_whenTheCallerIsAStudent() throws Exception {
        Student attacker = register("Sneaky", "One");
        Student victim = register("Vic", "Tim");

        mvc.perform(delete("/api/staff/applicants/" + pid(victim.applicantId())).header("Authorization", bearer(attacker.token())))
                .andExpect(status().isForbidden());

        assertThat(count("nad_applicant", "id", victim.applicantId())).isEqualTo(1);
    }

    @Test
    void shouldLeaveAnotherApplicantsOwnerLoginAlone_whenOnlyOneOfTheirApplicantsIsDeleted() throws Exception {
        Student s = register("Two", "Kids");
        long userId = userIdOf(s);
        createStaff("era_admin4", "nadoumi_super_admin");
        String second = mvc.perform(post("/api/student/applicants").header("Authorization", bearer(s.token()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"givenName\":\"Little\",\"familyName\":\"One\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long secondId = ((Number) JsonPath.read(second, "$.id")).longValue();

        mvc.perform(delete("/api/staff/applicants/" + pid(secondId)).header("Authorization", bearer(staffToken("era_admin4"))))
                .andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("select del_flag from sys_user where user_id = ?", String.class, userId)).isEqualTo("0");
        assertThat(count("nad_applicant", "id", s.applicantId())).isEqualTo(1);
    }

    @Test
    void shouldLeaveNoGhostChatOrNotificationBehind_whenTheStudentIsDeleted() throws Exception {
        Student s = register("Ghost", "Free");
        long userId = userIdOf(s);
        createStaff("era_admin5", "nadoumi_super_admin");
        String staff = bearer(staffToken("era_admin5"));
        mvc.perform(post("/api/staff/conversations/direct").header("Authorization", staff)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":" + userId + "}"))
                .andExpect(status().isOk());
        jdbc.update("insert into nad_notification (recipient_user_id, type, title, body, created_at) "
                + "values (?, 'WELCOME', 'Welcome', 'Hello', now())", userId);
        assertThat(count("nad_conversation_participant", "user_id", userId)).isEqualTo(1);

        mvc.perform(delete("/api/staff/applicants/" + pid(s.applicantId())).header("Authorization", staff))
                .andExpect(status().isNoContent());

        assertThat(count("nad_conversation_participant", "user_id", userId)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from nad_conversation", Long.class)).isZero();
        assertThat(count("nad_notification", "recipient_user_id", userId)).isZero();
    }

    @Test
    void shouldLetTheSameStudentRegisterAgainWithTheSameUsernameAndEmail_afterTheyWereDeleted() throws Exception {
        String email = uniqueEmail();
        String username = usernameFor("again", "same");
        String ticket = ticketFor(email);
        mvc.perform(post("/api/student/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"email\":\"" + email + "\",\"password\":\""
                                + STRONG_PASSWORD + "\",\"ticket\":\"" + ticket + "\"}"))
                .andExpect(status().isCreated());
        long applicantId = jdbc.queryForObject(
                "select a.id from nad_applicant a join sys_user u on u.user_id = a.create_by where u.email = ?", Long.class, email);
        createStaff("era_admin6", "nadoumi_super_admin");

        mvc.perform(delete("/api/staff/applicants/" + pid(applicantId)).header("Authorization", bearer(staffToken("era_admin6"))))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/student/username-available").param("username", username))
                .andExpect(jsonPath("$.available").value(true));
        assertThat(jdbc.queryForObject("select count(*) from sys_user where email = ? and del_flag = '0'", Integer.class, email))
                .isZero();
        String newEmail = uniqueEmail(); // the 60 s resend cooldown applies per address, so use a new one for the code
        String again = ticketFor(newEmail);
        mvc.perform(post("/api/student/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"email\":\"" + newEmail + "\",\"password\":\""
                                + STRONG_PASSWORD + "\",\"ticket\":\"" + again + "\"}"))
                .andExpect(status().isCreated());
    }
}
