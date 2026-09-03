package com.ruoyi.nadoumi;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Scholarship staff CRUD, public discovery, and the required confidentiality
 * tests: an anonymous request can never retrieve the partner-university linkage
 * or any {@code nad_scholarship_internal} field, and the internal sub-resource
 * is gated by {@code nad:scholarship:internal:*}.
 */
class StaffScholarshipTest extends AbstractNadIntegrationTest {

    private static final String BODY = """
            {"title":"CSC Full Scholarship — Master","country":"cn","field":"Engineering",
             "teachingLanguage":"ENGLISH","fundingModel":"FULLY","hasStipend":true,
             "deadline":"2026-03-31","benefits":"Tuition, accommodation, stipend, insurance.",
             "requirements":"Bachelor degree, IELTS 6.0.","status":"ACTIVE","publishStatus":"PUBLISHED",
             "levels":["MASTER","PHD"],"categoryCodes":["CSC","TYPE_A"],
             "intakes":[{"term":"AUTUMN_SEPTEMBER","applicationOpen":"2025-12-01","applicationClose":"2026-03-31"}],
             "eligibility":{"ageMin":18,"ageMax":35,"gpaMin":3.0,"ieltsMin":6.0,"nationalityScope":"ANY"},
             "fees":[{"kind":"APPLICATION","amount":800.00,"currency":"CNY","note":"Non-refundable"},
                     {"kind":"NADOUMI_SERVICE","amount":300.00,"currency":"USD"}],
             "stipend":{"amount":3500.00,"currency":"CNY","frequency":"MONTHLY","durationMonths":36},
             "documentRequirements":[{"docType":"PASSPORT","mandatory":true},
                                     {"docType":"DEGREE","mandatory":true},
                                     {"docType":"STUDY_PLAN","mandatory":false,"note":"1–2 pages"}]}""";

    private long createPublished(String token) throws Exception {
        String created = mvc.perform(post("/api/staff/scholarships").header("Authorization", bearer(token))
                        .contentType("application/json").content(BODY))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(created, "$.view.id")).longValue();
    }

    @Test
    void full_scholarship_crud_with_every_child_collection() throws Exception {
        createStaff("sch_ops", "nadoumi_super_admin");
        String token = staffToken("sch_ops");

        String created = mvc.perform(post("/api/staff/scholarships").header("Authorization", bearer(token))
                        .contentType("application/json").content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.view.title").value("CSC Full Scholarship — Master"))
                .andExpect(jsonPath("$.view.slug").value("csc-full-scholarship-master"))
                .andExpect(jsonPath("$.view.country").value("CN"))
                .andExpect(jsonPath("$.view.levels.length()").value(2))
                .andExpect(jsonPath("$.view.categories.length()").value(2))
                .andExpect(jsonPath("$.view.intakes.length()").value(1))
                .andExpect(jsonPath("$.view.fees.length()").value(2))
                .andExpect(jsonPath("$.view.stipend.amount").value(3500.00))
                .andExpect(jsonPath("$.view.documentRequirements.length()").value(3))
                .andExpect(jsonPath("$.publishStatus").value("PUBLISHED"))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(created, "$.view.id")).longValue();

        mvc.perform(put("/api/staff/scholarships/{id}", id).header("Authorization", bearer(token))
                        .contentType("application/json").content("""
                            {"title":"CSC Full Scholarship — Master","country":"CN","fundingModel":"PARTIAL",
                             "hasStipend":false,"status":"ACTIVE","publishStatus":"DRAFT",
                             "levels":["MASTER"],"categoryCodes":["CSC"],"fees":[]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.view.fundingModel").value("PARTIAL"))
                .andExpect(jsonPath("$.view.levels.length()").value(1))
                .andExpect(jsonPath("$.view.fees.length()").value(0))
                .andExpect(jsonPath("$.publishStatus").value("DRAFT"));

        mvc.perform(delete("/api/staff/scholarships/{id}", id).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/staff/scholarships/{id}", id).header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void read_only_staff_can_list_but_not_create() throws Exception {
        createStaff("sch_ro", "case_officer");
        String token = staffToken("sch_ro");

        mvc.perform(get("/api/staff/scholarships").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/staff/scholarships").header("Authorization", bearer(token))
                        .contentType("application/json").content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void the_public_discovery_never_exposes_the_partner_university_or_any_confidential_field() throws Exception {
        createStaff("sch_pub", "nadoumi_super_admin");
        String token = staffToken("sch_pub");

        // a real university to link confidentially
        String uni = mvc.perform(post("/api/staff/universities").header("Authorization", bearer(token))
                        .contentType("application/json").content("""
                            {"name":"Partner University","country":"CN","status":"ACTIVE","publishStatus":"PUBLISHED"}"""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long uniId = ((Number) JsonPath.read(uni, "$.id")).longValue();

        long pubId = createPublished(token);

        // staff sets the CONFIDENTIAL linkage
        mvc.perform(put("/api/staff/scholarships/{id}/internal", pubId).header("Authorization", bearer(token))
                        .contentType("application/json").content("""
                            {"universityId":%d,"internalStatus":"NEGOTIATING",
                             "operationalNotes":"Contact Prof. Wang","confidentialTerms":"18%% commission",
                             "commissionModelJson":"{\\"rate\\":0.18}"}""".formatted(uniId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.universityId").value((int) uniId));

        // a DRAFT scholarship must not appear
        mvc.perform(post("/api/staff/scholarships").header("Authorization", bearer(token))
                .contentType("application/json").content("""
                    {"title":"Hidden Draft","country":"CN","fundingModel":"SELF",
                     "hasStipend":false,"status":"ACTIVE","publishStatus":"DRAFT"}""")).andExpect(status().isCreated());

        // anonymous list — no auth header
        String listJson = mvc.perform(get("/api/public/scholarships"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value((int) pubId))
                .andExpect(jsonPath("$.content[0].title").value("CSC Full Scholarship — Master"))
                .andReturn().getResponse().getContentAsString();
        assertNoConfidentialKeys(listJson);

        // anonymous detail by slug
        String detailJson = mvc.perform(get("/api/public/scholarships/{slug}", "csc-full-scholarship-master"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fees.length()").value(2))
                .andExpect(jsonPath("$.documentRequirements.length()").value(3))
                .andExpect(jsonPath("$.universityId").doesNotExist())
                .andExpect(jsonPath("$.partnership").doesNotExist())
                .andExpect(jsonPath("$.internal").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        assertNoConfidentialKeys(detailJson);

        // the linked university's name / id must not leak either
        mvc.perform(get("/api/public/scholarships/{slug}", "csc-full-scholarship-master"))
                .andExpect(content().string(not(containsString("Partner University"))))
                .andExpect(content().string(not(containsString("commission"))))
                .andExpect(content().string(not(containsString("Prof. Wang"))));
    }

    @Test
    void the_confidential_linkage_sub_resource_is_gated_by_the_internal_permission() throws Exception {
        createStaff("sch_admin", "nadoumi_super_admin");
        String adminToken = staffToken("sch_admin");
        createStaff("sch_case", "case_officer");
        String caseToken = staffToken("sch_case");

        long id = createPublished(adminToken);

        // case_officer has scholarship:internal:view (per PERMISSION_CATALOGUE) …
        mvc.perform(get("/api/staff/scholarships/{id}/internal", id).header("Authorization", bearer(caseToken)))
                .andExpect(status().isOk());
        // … but not scholarship:internal:edit
        mvc.perform(put("/api/staff/scholarships/{id}/internal", id).header("Authorization", bearer(caseToken))
                        .contentType("application/json").content("{\"internalStatus\":\"DRAFT\"}"))
                .andExpect(status().isForbidden());
    }

    private static void assertNoConfidentialKeys(String json) {
        for (String needle : new String[] {
                "universityId", "university_id", "partnershipId", "partnership",
                "operationalNotes", "confidentialTerms", "commission", "internalStatus", "internal" }) {
            org.assertj.core.api.Assertions.assertThat(json)
                    .as("public scholarship JSON must not contain '%s'", needle)
                    .doesNotContain(needle);
        }
    }
}
