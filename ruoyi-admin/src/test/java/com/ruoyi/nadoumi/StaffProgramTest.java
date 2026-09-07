package com.ruoyi.nadoumi;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Staff programme catalog: CRUD + children round-trip, the university link is
 * validated / resolved through UniversityService, the unique guard, the
 * permission gate, and public visibility (a programme is only public when both it
 * and its university are PUBLISHED + ACTIVE).
 */
class StaffProgramTest extends AbstractNadIntegrationTest {

    private long createUniversity(String token, String name, String publishStatus) throws Exception {
        String body = """
                {"name":"%s","country":"CN","type":"PUBLIC","status":"ACTIVE","publishStatus":"%s"}"""
                .formatted(name, publishStatus);
        String res = mvc.perform(post("/api/staff/universities").header("Authorization", bearer(token))
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(res, "$.id")).longValue();
    }

    private static String programBody(long universityId, String publishStatus) {
        return """
                {"universityId":%d,"name":"  MBA  ","nameCn":"工商管理硕士","programType":"DEGREE","levels":["MASTER"],
                 "field":"Business","teachingLanguage":"ENGLISH","durationMonths":24,
                 "tuitionAmount":38000.00,"tuitionCurrency":"usd","summary":"A two-year MBA.",
                 "status":"ACTIVE","publishStatus":"%s",
                 "majors":[{"name":"Finance"},{"name":"Marketing"}],
                 "intakes":[{"term":"AUTUMN_SEPTEMBER","applicationOpen":"2026-03-01","applicationClose":"2026-06-30"}]}"""
                .formatted(universityId, publishStatus);
    }

    @Test
    void full_crud_with_children_and_resolved_university_name() throws Exception {
        createStaff("prog_ops", "nadoumi_super_admin");
        String token = staffToken("prog_ops");
        long uni = createUniversity(token, "Fudan University", "PUBLISHED");

        String created = mvc.perform(post("/api/staff/programs").header("Authorization", bearer(token))
                        .contentType("application/json").content(programBody(uni, "PUBLISHED")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("MBA"))
                .andExpect(jsonPath("$.slug").value("fudan-university-mba"))
                .andExpect(jsonPath("$.universityId").value((int) uni))
                .andExpect(jsonPath("$.universityName").value("Fudan University"))
                .andExpect(jsonPath("$.tuitionCurrency").value("USD"))
                .andExpect(jsonPath("$.majors.length()").value(2))
                .andExpect(jsonPath("$.programType").value("DEGREE"))
                .andExpect(jsonPath("$.levels[0]").value("MASTER"))
                .andExpect(jsonPath("$.intakes.length()").value(1))
                .andExpect(jsonPath("$.intakes[0].term").value("AUTUMN_SEPTEMBER"))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(created, "$.id")).longValue();

        // edit: drop one major, clear intakes, flip publish off
        mvc.perform(put("/api/staff/programs/{id}", id).header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content("""
                            {"universityId":%d,"name":"MBA","programType":"DEGREE","levels":["MASTER"],"status":"ACTIVE",
                             "publishStatus":"DRAFT","majors":[{"name":"Finance"}],"intakes":[]}""".formatted(uni)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishStatus").value("DRAFT"))
                .andExpect(jsonPath("$.majors.length()").value(1))
                .andExpect(jsonPath("$.intakes.length()").value(0));

        mvc.perform(delete("/api/staff/programs/{id}", id).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/staff/programs/{id}", id).header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void an_unknown_university_is_rejected() throws Exception {
        createStaff("prog_baduni", "nadoumi_super_admin");
        String token = staffToken("prog_baduni");

        mvc.perform(post("/api/staff/programs").header("Authorization", bearer(token))
                        .contentType("application/json").content(programBody(999999, "DRAFT")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void a_duplicate_name_under_the_same_university_is_rejected() throws Exception {
        createStaff("prog_dup", "nadoumi_super_admin");
        String token = staffToken("prog_dup");
        long uni = createUniversity(token, "Peking University", "PUBLISHED");

        mvc.perform(post("/api/staff/programs").header("Authorization", bearer(token))
                .contentType("application/json").content(programBody(uni, "DRAFT"))).andExpect(status().isCreated());
        mvc.perform(post("/api/staff/programs").header("Authorization", bearer(token))
                        .contentType("application/json").content(programBody(uni, "DRAFT")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void read_only_staff_can_list_but_not_create() throws Exception {
        createStaff("prog_ro", "case_officer");
        String token = staffToken("prog_ro");

        mvc.perform(get("/api/staff/programs").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/staff/programs").header("Authorization", bearer(token))
                        .contentType("application/json").content(programBody(1, "DRAFT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void public_endpoints_require_both_the_programme_and_its_university_to_be_public() throws Exception {
        createStaff("prog_pub", "nadoumi_super_admin");
        String token = staffToken("prog_pub");
        long publicUni = createUniversity(token, "Public University", "PUBLISHED");
        long draftUni = createUniversity(token, "Draft University", "DRAFT");

        // published programme under a published university -> visible
        String pub = mvc.perform(post("/api/staff/programs").header("Authorization", bearer(token))
                        .contentType("application/json").content(programBody(publicUni, "PUBLISHED")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long pubId = ((Number) JsonPath.read(pub, "$.id")).longValue();

        // draft programme under the same published university -> hidden
        mvc.perform(post("/api/staff/programs").header("Authorization", bearer(token))
                .contentType("application/json").content("""
                    {"universityId":%d,"name":"Hidden Programme","programType":"DEGREE","levels":["BACHELOR"],
                     "status":"ACTIVE","publishStatus":"DRAFT"}""".formatted(publicUni)))
                .andExpect(status().isCreated());

        // published programme under a DRAFT university -> hidden
        mvc.perform(post("/api/staff/programs").header("Authorization", bearer(token))
                .contentType("application/json").content("""
                    {"universityId":%d,"name":"Orphan Programme","programType":"DEGREE","levels":["BACHELOR"],
                     "status":"ACTIVE","publishStatus":"PUBLISHED"}""".formatted(draftUni)))
                .andExpect(status().isCreated());

        // anonymous list — only the one fully-public programme, no operational fields
        mvc.perform(get("/api/public/programs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value((int) pubId))
                .andExpect(jsonPath("$.content[0].universityName").value("Public University"))
                .andExpect(jsonPath("$.content[0].status").doesNotExist())
                .andExpect(jsonPath("$.content[0].publishStatus").doesNotExist())
                .andExpect(jsonPath("$.content[0].remark").doesNotExist());

        // per-university list
        mvc.perform(get("/api/public/universities/{id}/programs", publicUni))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value((int) pubId));

        // per-university list for a draft university -> 404
        mvc.perform(get("/api/public/universities/{id}/programs", draftUni))
                .andExpect(status().isNotFound());

        // detail with children — by id and by slug
        mvc.perform(get("/api/public/programs/{id}", pubId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("public-university-mba"))
                .andExpect(jsonPath("$.majors.length()").value(2))
                .andExpect(jsonPath("$.intakes.length()").value(1));
        mvc.perform(get("/api/public/programs/{slug}", "public-university-mba"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) pubId));
    }
}
