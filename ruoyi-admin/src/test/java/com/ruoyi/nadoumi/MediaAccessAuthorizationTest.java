package com.ruoyi.nadoumi;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Delivery authorization for media (spec §I.6):
 * <ul>
 *   <li>PUBLIC assets are reachable anonymously through the stable
 *       {@code GET /api/media/{id}} indirection ({@code 302} to the secure URL);</li>
 *   <li>a PROTECTED applicant photo is never disclosed on that public route — it
 *       {@code 404}s exactly as a missing id would;</li>
 *   <li>the authorized photo route hands an entitled caller a short-TTL signed URL
 *       (JSON or {@code 302}), and rejects a caller without
 *       {@code nad:applicant:view} at the gate.</li>
 * </ul>
 *
 * <p>Staff-only flows on purpose: {@code /api/student/login} is globally
 * rate-limited ({@code @RateLimiter(count = 10, time = 60)}), so adding student
 * logins here would starve the pre-existing student-auth ITs. Grant-revocation of
 * the photo route is covered by {@code RevocationTakesEffectNextRequestTest} —
 * the staff photo GET is gated by the plain {@code nad:applicant:view} permission,
 * not a per-applicant grant.</p>
 */
class MediaAccessAuthorizationTest extends AbstractNadIntegrationTest {

    private static final byte[] PNG = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4, (byte) 0x89
    };

    @Test
    void publicMediaServedToAnonymous() throws Exception {
        createStaff("maa_pub", "ops_manager");
        String token = staffToken("maa_pub");
        long uniId = createUniversity(token);

        String uploaded = mvc.perform(multipart("/api/staff/universities/{id}/logo", uniId)
                        .file(pngPart()).header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long mediaId = ((Number) JsonPath.read(uploaded, "$.mediaId")).longValue();
        String url = JsonPath.read(uploaded, "$.url");

        mvc.perform(get("/api/media/{id}", mediaId))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(url));
    }

    @Test
    void protectedMediaNeverViaPublicRoute() throws Exception {
        createStaff("maa_prot", "ops_manager");
        String token = staffToken("maa_prot");
        long applicantId = createApplicant(token);
        long mediaId = uploadStaffPhoto(token, applicantId);

        // the public indirection must not disclose a PROTECTED asset — 404, like a missing id
        mvc.perform(get("/api/media/{id}", mediaId))
                .andExpect(status().isNotFound());
    }

    @Test
    void entitledStaffGetsSignedUrl() throws Exception {
        long staffUserId = createStaff("maa_owner", "ops_manager");
        String token = staffToken("maa_owner");
        long applicantId = createApplicant(token);
        long mediaId = uploadStaffPhoto(token, applicantId);

        mvc.perform(get("/api/staff/applicants/{id}/photo", applicantId)
                        .param("json", "1").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(startsWith("https://fake.local/")))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());

        var row = jdbc.queryForMap("select media_asset_id, actor_user_id, access_kind, result, ttl_seconds "
                + "from nad_media_access_log order by id desc limit 1");
        assertThat(((Number) row.get("media_asset_id")).longValue()).isEqualTo(mediaId);
        assertThat(((Number) row.get("actor_user_id")).longValue()).isEqualTo(staffUserId);
        assertThat(row.get("access_kind")).isEqualTo("SIGNED_URL_ISSUED");
        assertThat(row.get("result")).isEqualTo("GRANTED");
        assertThat(((Number) row.get("ttl_seconds")).intValue()).isPositive();
    }

    @Test
    void photoGetRedirectsWhenJsonNotRequested() throws Exception {
        createStaff("maa_redir", "ops_manager");
        String token = staffToken("maa_redir");
        long applicantId = createApplicant(token);
        uploadStaffPhoto(token, applicantId);

        mvc.perform(get("/api/staff/applicants/{id}/photo", applicantId).header("Authorization", bearer(token)))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", startsWith("https://fake.local/")));
    }

    @Test
    void callerWithoutApplicantViewForbidden() throws Exception {
        createStaff("maa_ph_owner", "ops_manager");
        String ownerToken = staffToken("maa_ph_owner");
        long applicantId = createApplicant(ownerToken);
        uploadStaffPhoto(ownerToken, applicantId); // a real photo exists

        // bespoke staff role with no menu grants -> lacks nad:applicant:view
        jdbc.update("insert into sys_role (role_name, role_key, role_sort, data_scope, status, create_by, create_time) "
                + "values ('Media no applicant view','media_no_appl_view',97,'1','0','test',now()) "
                + "on duplicate key update role_key = role_key");
        createStaff("maa_noview", "media_no_appl_view");
        String noViewToken = staffToken("maa_noview");

        mvc.perform(get("/api/staff/applicants/{id}/photo", applicantId)
                        .param("json", "1").header("Authorization", bearer(noViewToken)))
                .andExpect(status().isForbidden());

        // The @PreAuthorize('nad:applicant:view') gate rejects before
        // ApplicantService#photoUrl runs, so no audit row is written.
        // denyAndLog(NO_APPLICANT_GRANT) is the defensive TOCTOU branch — a grant
        // revoked between the gate check and the in-service re-check.
        assertThat(jdbc.queryForObject("select count(*) from nad_media_access_log", Integer.class)).isZero();
    }

    // ---- helpers ----

    private static org.springframework.mock.web.MockMultipartFile pngPart() {
        return new org.springframework.mock.web.MockMultipartFile("file", "logo.png", "image/png", PNG);
    }

    private long createUniversity(String token) throws Exception {
        String res = mvc.perform(post("/api/staff/universities").header("Authorization", bearer(token))
                        .contentType("application/json").content("""
                            {"name":"Media Authz University","country":"CN","type":"PUBLIC",
                             "status":"ACTIVE","publishStatus":"PUBLISHED"}"""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(res, "$.id")).longValue();
    }

    private long createApplicant(String staffToken) throws Exception {
        String res = mvc.perform(post("/api/staff/applicants").header("Authorization", bearer(staffToken))
                        .contentType("application/json")
                        .content("{\"givenName\":\"Photo\",\"familyName\":\"Owner\",\"invitedEmail\":\"po@example.test\"}"))
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
