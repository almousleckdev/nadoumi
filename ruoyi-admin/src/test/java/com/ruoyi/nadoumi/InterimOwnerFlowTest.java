package com.ruoyi.nadoumi;

import com.jayway.jsonpath.JsonPath;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Staff-created applicant: interim staff owner + PENDING owner invite; the invitee
 * registering and signing in promotes the invite and revokes the interim grant
 * (DOMAIN_MODEL §4.3).
 */
class InterimOwnerFlowTest extends AbstractNadIntegrationTest {

    @Test
    void staffCreateThenInviteeAcceptance() throws Exception {
        long staffId = createStaff("interim_staff", "ops_manager");
        String staffTok = staffToken("interim_staff");

        String created = mvc.perform(post("/api/staff/applicants").header("Authorization", bearer(staffTok))
                        .contentType("application/json").content("""
                                {"givenName":"Amina","familyName":"Diallo","invitedEmail":"invitee@example.test"}"""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long applicantId = ((Number) JsonPath.read(created, "$.id")).longValue();

        var rows = jdbc.queryForList(
                "select user_id, access_role, status, is_interim, invited_email "
                        + "from nad_user_applicant_access where applicant_id = ? order by status", applicantId);
        assertThat(rows).hasSize(2);
        Map<String, Object> active = rows.stream()
                .filter(r -> "ACTIVE".equals(r.get("status"))).findFirst().orElseThrow();
        assertThat(active.get("access_role")).isEqualTo("OWNER");
        assertThat((Boolean) active.get("is_interim")).isTrue();
        assertThat(((Number) active.get("user_id")).longValue()).isEqualTo(staffId);
        Map<String, Object> pending = rows.stream()
                .filter(r -> "PENDING".equals(r.get("status"))).findFirst().orElseThrow();
        assertThat(pending.get("invited_email")).isEqualTo("invitee@example.test");
        assertThat(pending.get("user_id")).isNull();

        mvc.perform(post("/api/student/register").contentType("application/json").content("""
                        {"username":"invitee","password":"nad-test-pass-1","email":"invitee@example.test"}"""))
                .andExpect(status().isCreated());
        String inviteeTok = studentToken("invitee");

        Map<String, Object> promoted = jdbc.queryForMap(
                "select user_id, is_interim from nad_user_applicant_access "
                        + "where applicant_id = ? and access_role = 'OWNER' and status = 'ACTIVE'", applicantId);
        assertThat((Boolean) promoted.get("is_interim")).isFalse();
        assertThat(((Number) promoted.get("user_id")).longValue()).isNotEqualTo(staffId);

        String revokeReason = jdbc.queryForObject(
                "select revoke_reason from nad_user_applicant_access "
                        + "where applicant_id = ? and status = 'REVOKED'", String.class, applicantId);
        assertThat(revokeReason).isEqualTo("ownership_transferred");

        mvc.perform(get("/api/student/me").header("Authorization", bearer(inviteeTok)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessibleApplicants[0].accessRole").value("OWNER"));
    }
}
