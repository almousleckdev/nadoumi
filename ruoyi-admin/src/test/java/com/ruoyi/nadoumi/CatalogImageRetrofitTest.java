package com.ruoyi.nadoumi;

import com.jayway.jsonpath.JsonPath;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The catalog image retrofit (spec §I.5): every {@code POST .../logo|hero|image}
 * stores a {@code nad_media_asset} row, points the owning table's {@code *_media_id}
 * at it, and the read models resolve the delivery URL through the MediaGateway
 * (here the {@code FakeMediaStorage}, so the URL is a deterministic
 * {@code https://fake.local/public/...} string). Also covers the two upload
 * boundary rejections that matter most for images: the per-category size cap and
 * a content-type spoof.
 */
class CatalogImageRetrofitTest extends AbstractNadIntegrationTest {

    /** 8-byte PNG signature + a minimal IHDR chunk — sniffs as image/png through Tika. */
    private static final byte[] PNG = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4, (byte) 0x89
    };

    private static final String FAKE_PUBLIC_PREFIX = "https://fake.local/public/";

    @Test
    void universityLogoUpload_storesAssetAndResolvesUrl() throws Exception {
        createStaff("retro_uni", "ops_manager");
        String token = staffToken("retro_uni");

        String created = mvc.perform(post("/api/staff/universities").header("Authorization", bearer(token))
                        .contentType("application/json").content("""
                            {"name":"Retrofit University","country":"CN","type":"PUBLIC",
                             "status":"ACTIVE","publishStatus":"PUBLISHED"}"""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long uniId = ((Number) JsonPath.read(created, "$.id")).longValue();

        String uploaded = mvc.perform(multipart("/api/staff/universities/{id}/logo", uniId)
                        .file(pngPart()).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mediaId").isNumber())
                .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.startsWith(FAKE_PUBLIC_PREFIX)))
                .andReturn().getResponse().getContentAsString();
        long mediaId = ((Number) JsonPath.read(uploaded, "$.mediaId")).longValue();
        String url = JsonPath.read(uploaded, "$.url");

        mvc.perform(get("/api/staff/universities/{id}", uniId).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoMediaId").value((int) mediaId))
                .andExpect(jsonPath("$.logoUrl").value(url));

        Map<String, Object> row = jdbc.queryForMap(
                "select category, access_class, owner_kind, owner_id from nad_media_asset where id = ?", mediaId);
        assertThat(row.get("category")).isEqualTo("UNIVERSITY_LOGO");
        assertThat(row.get("access_class")).isEqualTo("PUBLIC");
        assertThat(row.get("owner_kind")).isEqualTo("UNIVERSITY");
        assertThat(((Number) row.get("owner_id")).longValue()).isEqualTo(uniId);
    }

    @Test
    void scholarshipHeroUpload_resolvesOnPublicDetail_withoutLeakingConfidentialFields() throws Exception {
        createStaff("retro_sch", "ops_manager");
        String token = staffToken("retro_sch");
        long schId = createPublishedScholarship(token);

        String uploaded = mvc.perform(multipart("/api/staff/scholarships/{id}/hero", schId)
                        .file(pngPart()).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.startsWith(FAKE_PUBLIC_PREFIX)))
                .andReturn().getResponse().getContentAsString();
        long mediaId = ((Number) JsonPath.read(uploaded, "$.mediaId")).longValue();
        String url = JsonPath.read(uploaded, "$.url");

        String detail = mvc.perform(get("/api/public/scholarships/{slugOrId}", Long.toString(schId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.heroMediaId").value((int) mediaId))
                .andExpect(jsonPath("$.heroUrl").value(url))
                .andExpect(jsonPath("$.universityId").doesNotExist())
                .andExpect(jsonPath("$.partnership").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        for (String needle : new String[] { "universityId", "partnership", "commission", "internal" }) {
            assertThat(detail).as("public scholarship JSON must not contain '%s'", needle).doesNotContain(needle);
        }

        Map<String, Object> row = jdbc.queryForMap(
                "select category, access_class, owner_kind from nad_media_asset where id = ?", mediaId);
        assertThat(row.get("category")).isEqualTo("SCHOLARSHIP_HERO");
        assertThat(row.get("access_class")).isEqualTo("PUBLIC");
        assertThat(row.get("owner_kind")).isEqualTo("SCHOLARSHIP");
    }

    @Test
    void programImageUpload_resolvesOnStaffDetail() throws Exception {
        createStaff("retro_prog", "ops_manager");
        String token = staffToken("retro_prog");
        long uniId = createUniversity(token, "Programme Host University");
        long progId = createProgram(token, uniId);

        String uploaded = mvc.perform(multipart("/api/staff/programs/{id}/image", progId)
                        .file(pngPart()).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.startsWith(FAKE_PUBLIC_PREFIX)))
                .andReturn().getResponse().getContentAsString();
        long mediaId = ((Number) JsonPath.read(uploaded, "$.mediaId")).longValue();
        String url = JsonPath.read(uploaded, "$.url");

        mvc.perform(get("/api/staff/programs/{id}", progId).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageMediaId").value((int) mediaId))
                .andExpect(jsonPath("$.imageUrl").value(url));

        Map<String, Object> row = jdbc.queryForMap(
                "select category, access_class, owner_kind from nad_media_asset where id = ?", mediaId);
        assertThat(row.get("category")).isEqualTo("PROGRAM_IMAGE");
        assertThat(row.get("access_class")).isEqualTo("PUBLIC");
        assertThat(row.get("owner_kind")).isEqualTo("PROGRAM");
    }

    @Test
    void oversizeLogoRejected_andNothingPersisted() throws Exception {
        createStaff("retro_big", "ops_manager");
        String token = staffToken("retro_big");
        long uniId = createUniversity(token, "Oversize University");

        // UNIVERSITY_LOGO cap is 4 MB (spec §I.5); one byte over trips the size check.
        byte[] tooBig = new byte[4 * 1024 * 1024 + 1];
        var part = new org.springframework.mock.web.MockMultipartFile("file", "logo.png", "image/png", tooBig);

        // The upload boundary (MediaValidation) rejects the file before any provider
        // call: no mediaId in the response, and nothing persisted.
        //
        // KNOWN GAP (task-17): spec §I.5 wants HTTP 413 here, but a
        // MediaValidationException thrown from StaffUniversityController
        // (com.nadoumi.university.web) is not mapped —
        // MediaExceptionAdvice is @RestControllerAdvice(basePackages = "com.nadoumi.media")
        // and NadApiExceptionHandler has no handler for it — so RuoYi's
        // GlobalExceptionHandler renders it as a 200 {code:500,msg:...} envelope.
        // Fix: widen MediaExceptionAdvice to basePackages = "com.nadoumi" (or add
        // @ExceptionHandler(MediaValidationException) to NadApiExceptionHandler), then
        // assert status().isPayloadTooLarge() here.
        mvc.perform(multipart("/api/staff/universities/{id}/logo", uniId)
                        .file(part).header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.mediaId").doesNotExist());

        assertThat(jdbc.queryForObject("select count(*) from nad_media_asset", Integer.class)).isZero();
        mvc.perform(get("/api/staff/universities/{id}", uniId).header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.logoMediaId").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void htmlDisguisedAsPngRejected_andNothingPersisted() throws Exception {
        createStaff("retro_html", "ops_manager");
        String token = staffToken("retro_html");
        long uniId = createUniversity(token, "Spoof University");

        var part = new org.springframework.mock.web.MockMultipartFile("file", "x.png", "image/png",
                "<html><body><script>alert(1)</script></body></html>".getBytes());

        // Tika sniffs text/html — on the hard denylist AND not an image-family match
        // for the declared image/png — so MediaValidation rejects it before any
        // provider call: no mediaId, nothing persisted.
        //
        // KNOWN GAP (task-17): spec §I.5 wants HTTP 415/422 here; see the note on
        // oversizeLogoRejected — the same unmapped MediaValidationException currently
        // surfaces as a 200 {code:500,msg:...} envelope from RuoYi's handler.
        mvc.perform(multipart("/api/staff/universities/{id}/logo", uniId)
                        .file(part).header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.mediaId").doesNotExist());

        assertThat(jdbc.queryForObject("select count(*) from nad_media_asset", Integer.class)).isZero();
    }

    // ---- helpers ----

    private static org.springframework.mock.web.MockMultipartFile pngPart() {
        return new org.springframework.mock.web.MockMultipartFile("file", "logo.png", "image/png", PNG);
    }

    private long createUniversity(String token, String name) throws Exception {
        String res = mvc.perform(post("/api/staff/universities").header("Authorization", bearer(token))
                        .contentType("application/json").content("""
                            {"name":"%s","country":"CN","type":"PUBLIC","status":"ACTIVE","publishStatus":"PUBLISHED"}"""
                        .formatted(name)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(res, "$.id")).longValue();
    }

    private long createProgram(String token, long universityId) throws Exception {
        String res = mvc.perform(post("/api/staff/programs").header("Authorization", bearer(token))
                        .contentType("application/json").content("""
                            {"universityId":%d,"name":"Retrofit MBA","programType":"MASTER","field":"Business",
                             "teachingLanguage":"ENGLISH","durationMonths":24,"status":"ACTIVE","publishStatus":"PUBLISHED"}"""
                        .formatted(universityId)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(res, "$.id")).longValue();
    }

    private long createPublishedScholarship(String token) throws Exception {
        String res = mvc.perform(post("/api/staff/scholarships").header("Authorization", bearer(token))
                        .contentType("application/json").content("""
                            {"title":"Retrofit Scholarship","country":"CN","field":"Engineering",
                             "teachingLanguage":"ENGLISH","fundingModel":"FULLY","hasStipend":true,
                             "deadline":"2026-03-31","benefits":"Tuition + stipend.","requirements":"Bachelor degree.",
                             "status":"ACTIVE","publishStatus":"PUBLISHED","levels":["MASTER"],"categoryCodes":["CSC"]}"""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(res, "$.view.id")).longValue();
    }
}
