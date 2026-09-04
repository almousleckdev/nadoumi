package com.ruoyi.nadoumi;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code nad_media_access_log} discipline (spec §I.6): a PROTECTED signed-URL
 * issue appends exactly one {@code GRANTED} row; a request denied at the
 * authorization gate never reaches the audit path; and PUBLIC delivery through
 * {@code GET /api/media/{id}} is intentionally not audited.
 *
 * <p>Staff-only flows on purpose — see the note on {@code MediaAccessAuthorizationTest}
 * (the student login endpoint is globally rate-limited).</p>
 */
class MediaAccessLogTest extends AbstractNadIntegrationTest {

    private static final byte[] PNG = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4, (byte) 0x89
    };

    @Test
    void everySignedUrlIssueWritesOneGrantedRow() throws Exception {
        long staffUserId = createStaff("mal_grant", "ops_manager");
        String token = staffToken("mal_grant");
        long applicantId = createApplicant(token);
        long mediaId = uploadStaffPhoto(token, applicantId);

        assertThat(jdbc.queryForObject("select count(*) from nad_media_access_log", Integer.class)).isZero();

        mvc.perform(get("/api/staff/applicants/{id}/photo", applicantId)
                        .param("json", "1").header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        var rows = jdbc.queryForList("select media_asset_id, actor_user_id, access_kind, result, ttl_seconds "
                + "from nad_media_access_log");
        assertThat(rows).hasSize(1);
        var row = rows.get(0);
        assertThat(((Number) row.get("media_asset_id")).longValue()).isEqualTo(mediaId);
        assertThat(((Number) row.get("actor_user_id")).longValue()).isEqualTo(staffUserId);
        assertThat(row.get("access_kind")).isEqualTo("SIGNED_URL_ISSUED");
        assertThat(row.get("result")).isEqualTo("GRANTED");
        assertThat(((Number) row.get("ttl_seconds")).intValue()).isPositive();
    }

    @Test
    void deniedRequestAtGateDoesNotReachTheAuditPath() throws Exception {
        createStaff("mal_deny_owner", "ops_manager");
        String ownerToken = staffToken("mal_deny_owner");
        long applicantId = createApplicant(ownerToken);
        uploadStaffPhoto(ownerToken, applicantId); // a real photo exists

        jdbc.update("insert into sys_role (role_name, role_key, role_sort, data_scope, status, create_by, create_time) "
                + "values ('Media log no view','media_log_no_view',96,'1','0','test',now()) "
                + "on duplicate key update role_key = role_key");
        createStaff("mal_stranger", "media_log_no_view");
        String strangerToken = staffToken("mal_stranger");

        mvc.perform(get("/api/staff/applicants/{id}/photo", applicantId)
                        .param("json", "1").header("Authorization", bearer(strangerToken)))
                .andExpect(status().isForbidden());

        // The @PreAuthorize('nad:applicant:view') gate rejects an unentitled caller
        // before ApplicantService#photoUrl runs, so no DENIED row is written on this
        // path. denyAndLog(NO_APPLICANT_GRANT) is the defensive branch for a grant
        // revoked mid-request (a race the gate + in-service re-check together close).
        assertThat(jdbc.queryForObject("select count(*) from nad_media_access_log", Integer.class)).isZero();
    }

    @Test
    void publicMediaGetDoesNotLog() throws Exception {
        createStaff("mal_public", "ops_manager");
        String token = staffToken("mal_public");
        long uniId = createUniversity(token);

        String uploaded = mvc.perform(multipart("/api/staff/universities/{id}/logo", uniId)
                        .file(new org.springframework.mock.web.MockMultipartFile("file", "logo.png", "image/png", PNG))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long mediaId = ((Number) JsonPath.read(uploaded, "$.mediaId")).longValue();

        mvc.perform(get("/api/media/{id}", mediaId)).andExpect(status().isFound());

        assertThat(jdbc.queryForObject("select count(*) from nad_media_access_log", Integer.class)).isZero();
    }

    // ---- helpers ----

    private long createUniversity(String token) throws Exception {
        String res = mvc.perform(post("/api/staff/universities").header("Authorization", bearer(token))
                        .contentType("application/json").content("""
                            {"name":"Access Log University","country":"CN","type":"PUBLIC",
                             "status":"ACTIVE","publishStatus":"PUBLISHED"}"""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(res, "$.id")).longValue();
    }

    private long createApplicant(String staffToken) throws Exception {
        String res = mvc.perform(post("/api/staff/applicants").header("Authorization", bearer(staffToken))
                        .contentType("application/json")
                        .content("{\"givenName\":\"Log\",\"familyName\":\"Subject\",\"invitedEmail\":\"ls@example.test\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(res, "$.id")).longValue();
    }

    private long uploadStaffPhoto(String staffToken, long applicantId) throws Exception {
        String res = mvc.perform(multipart("/api/staff/applicants/{id}/photo", applicantId)
                        .file(new org.springframework.mock.web.MockMultipartFile("file", "me.png", "image/png", PNG))
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(res, "$.mediaId")).longValue();
    }
}
