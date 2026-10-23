package com.ruoyi.nadoumi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

/** The shared major catalogue: management, linking programme majors to it, merge and the in-use guard. */
class MajorCatalogueApiTest extends AbstractNadIntegrationTest {

    private long createMajor(String token, String body) throws Exception {
        String res = mvc.perform(post("/api/staff/majors").header("Authorization", bearer(token))
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(res, "$.id")).longValue();
    }

    private long createUniversity(String token) throws Exception {
        String res = mvc.perform(post("/api/staff/universities").header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content("{\"name\":\"Fudan University\",\"country\":\"CN\",\"type\":\"PUBLIC\",\"status\":\"ACTIVE\",\"publishStatus\":\"PUBLISHED\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(res, "$.id")).longValue();
    }

    private String programBody(long universityId, String name, String majors) {
        return "{\"universityId\":" + universityId + ",\"name\":\"" + name + "\",\"programType\":\"DEGREE\","
                + "\"levels\":[\"BACHELOR\",\"MASTER\"],\"status\":\"ACTIVE\",\"publishStatus\":\"PUBLISHED\",\"majors\":" + majors + "}";
    }

    @Test
    void shouldManageTheCatalogue_andRefuseADuplicateName() throws Exception {
        createStaff("major_ops", "nadoumi_super_admin");
        String token = staffToken("major_ops");

        createMajor(token, "{\"name\":\"Computer Science\",\"nameCn\":\"计算机科学\",\"discipline\":\"engineering\"}");
        mvc.perform(post("/api/staff/majors").header("Authorization", bearer(token))
                        .contentType("application/json").content("{\"name\":\"computer science\"}"))
                .andExpect(status().isBadRequest()); // names match regardless of case

        mvc.perform(get("/api/staff/majors?q=computer").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].discipline").value("ENGINEERING"))
                .andExpect(jsonPath("$.content[0].programCount").value(0));
    }

    @Test
    void shouldLinkProgrammeMajorsToTheCatalogue_whetherPickedOrTyped() throws Exception {
        createStaff("major_link", "nadoumi_super_admin");
        String token = staffToken("major_link");
        long uni = createUniversity(token);
        long finance = createMajor(token, "{\"name\":\"Finance\",\"nameCn\":\"金融学\"}");

        String created = mvc.perform(post("/api/staff/programs").header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(programBody(uni, "Business", "[{\"majorId\":" + finance + ",\"level\":\"MASTER\"},{\"name\":\"Marketing\",\"level\":\"BACHELOR\"}]")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.majors[0].majorId").value((int) finance))
                .andExpect(jsonPath("$.majors[0].name").value("Finance")) // the catalogue title
                .andExpect(jsonPath("$.majors[0].nameCn").value("金融学"))
                .andExpect(jsonPath("$.majors[1].name").value("Marketing"))
                .andReturn().getResponse().getContentAsString();
        long typedMajorId = ((Number) JsonPath.read(created, "$.majors[1].majorId")).longValue();

        // the typed major was added to the catalogue, and both are now in use
        mvc.perform(get("/api/staff/majors").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.name=='Marketing')].programCount").value(1));
        mvc.perform(delete("/api/staff/majors/" + typedMajorId).header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldMergeADuplicateIntoTheMajorToKeep() throws Exception {
        createStaff("major_merge", "nadoumi_super_admin");
        String token = staffToken("major_merge");
        long uni = createUniversity(token);
        long keep = createMajor(token, "{\"name\":\"Computer Science\"}");
        long duplicate = createMajor(token, "{\"name\":\"Computing Science\"}");
        mvc.perform(post("/api/staff/programs").header("Authorization", bearer(token))
                .contentType("application/json")
                .content(programBody(uni, "Tech", "[{\"majorId\":" + duplicate + ",\"level\":\"BACHELOR\"}]"))).andExpect(status().isCreated());

        mvc.perform(post("/api/staff/majors/" + duplicate + "/merge").header("Authorization", bearer(token))
                        .contentType("application/json").content("{\"intoId\":" + keep + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value((int) keep));

        mvc.perform(get("/api/staff/majors").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].programCount").value(1));
    }

    @Test
    void shouldCreateMissingMajorsFromAPastedList() throws Exception {
        createStaff("major_bulk", "nadoumi_super_admin");
        String token = staffToken("major_bulk");
        createMajor(token, "{\"name\":\"Law\"}");

        mvc.perform(post("/api/staff/majors/bulk").header("Authorization", bearer(token))
                        .contentType("application/json").content("{\"text\":\"Law\\nMedicine | 医学\\nNursing\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.created").value(2))
                .andExpect(jsonPath("$.majors.length()").value(3));
    }

    @Test
    void shouldRefuseCatalogueChangesWithoutThePermission() throws Exception {
        createStaff("major_view", "case_officer");
        String token = staffToken("major_view");

        mvc.perform(get("/api/staff/majors").header("Authorization", bearer(token))).andExpect(status().isOk());
        mvc.perform(post("/api/staff/majors").header("Authorization", bearer(token))
                .contentType("application/json").content("{\"name\":\"Law\"}")).andExpect(status().isForbidden());
    }
}
