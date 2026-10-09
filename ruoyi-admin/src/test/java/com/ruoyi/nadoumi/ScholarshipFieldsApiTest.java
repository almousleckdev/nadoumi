package com.ruoyi.nadoumi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;

/** A scholarship covers one or many fields of study, optionally per degree level. */
class ScholarshipFieldsApiTest extends AbstractNadIntegrationTest {

    private static String body(String title, String fieldsJson, String levelsJson, String publish, String legacyField) {
        return "{\"title\":\"" + title + "\",\"country\":\"CN\",\"fundingModel\":\"FULLY\",\"status\":\"ACTIVE\","
                + "\"publishStatus\":\"" + publish + "\",\"levels\":" + levelsJson
                + (fieldsJson == null ? "" : ",\"fields\":" + fieldsJson)
                + (legacyField == null ? "" : ",\"field\":\"" + legacyField + "\"") + "}";
    }

    private String token() throws Exception {
        createStaff("fld_staff", "nadoumi_super_admin");
        return bearer(staffToken("fld_staff"));
    }

    @Test
    void shouldStoreSeveralFieldsPerLevel_andReturnThemWithTheSearchSummary() throws Exception {
        String token = token();
        String fields = "[{\"name\":\"Engineering\"},{\"level\":\"BACHELOR\",\"name\":\"Architecture\"},"
                + "{\"level\":\"MASTER\",\"name\":\"Medicine\"},{\"level\":\"PHD\",\"name\":\"Medicine\"}]";

        mvc.perform(post("/api/staff/scholarships").header("Authorization", token).contentType("application/json")
                        .content(body("Multi field", fields, "[\"BACHELOR\",\"MASTER\",\"PHD\"]", "DRAFT", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.view.fields.length()").value(4))
                .andExpect(jsonPath("$.view.fields[0].name").value("Engineering"))
                .andExpect(jsonPath("$.view.fields[0].level").doesNotExist())
                .andExpect(jsonPath("$.view.fields[2].level").value("MASTER"))
                .andExpect(jsonPath("$.view.field").value("Engineering, Architecture, Medicine"));
    }

    @Test
    void shouldReplaceTheFieldsOnUpdate() throws Exception {
        String token = token();
        String created = mvc.perform(post("/api/staff/scholarships").header("Authorization", token).contentType("application/json")
                        .content(body("Replace me", "[{\"name\":\"Law\"},{\"name\":\"Economics\"}]", "[\"MASTER\"]", "DRAFT", null)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(created, "$.view.id")).longValue();

        mvc.perform(put("/api/staff/scholarships/" + id).header("Authorization", token).contentType("application/json")
                        .content(body("Replace me", "[{\"level\":\"MASTER\",\"name\":\"Education\"}]", "[\"MASTER\"]", "DRAFT", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.view.fields.length()").value(1))
                .andExpect(jsonPath("$.view.fields[0].name").value("Education"))
                .andExpect(jsonPath("$.view.field").value("Education"));
    }

    @Test
    void shouldRefuseAFieldForALevelTheScholarshipDoesNotOffer() throws Exception {
        String token = token();

        mvc.perform(post("/api/staff/scholarships").header("Authorization", token).contentType("application/json")
                        .content(body("Bad level", "[{\"level\":\"PHD\",\"name\":\"Physics\"}]", "[\"BACHELOR\"]", "DRAFT", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("does not offer")));
    }

    @Test
    void shouldStillAcceptTheOlderSingleField() throws Exception {
        String token = token();

        mvc.perform(post("/api/staff/scholarships").header("Authorization", token).contentType("application/json")
                        .content(body("Legacy", null, "[\"MASTER\"]", "DRAFT", "Engineering")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.view.fields.length()").value(1))
                .andExpect(jsonPath("$.view.fields[0].name").value("Engineering"))
                .andExpect(jsonPath("$.view.field").value("Engineering"));
    }

    @Test
    void shouldFindAPublishedScholarshipByAnyOfItsFields_butNotByAFieldItDoesNotHave() throws Exception {
        String token = token();
        mvc.perform(post("/api/staff/scholarships").header("Authorization", token).contentType("application/json")
                        .content(body("Findable", "[{\"name\":\"Engineering\"},{\"level\":\"MASTER\",\"name\":\"Medicine\"}]",
                                "[\"MASTER\"]", "PUBLISHED", null)))
                .andExpect(status().isCreated());

        for (String field : List.of("Engineering", "Medicine")) {
            String hit = mvc.perform(get("/api/public/scholarships").param("field", field))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(JsonPath.<List<String>>read(hit, "$.content[*].title")).as("filter by " + field).contains("Findable");
        }
        String miss = mvc.perform(get("/api/public/scholarships").param("field", "Law"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<String>>read(miss, "$.content[*].title")).doesNotContain("Findable");
    }

    @Test
    void shouldShowTheFieldsToStudentsOnThePublicDetail() throws Exception {
        String token = token();
        String created = mvc.perform(post("/api/staff/scholarships").header("Authorization", token).contentType("application/json")
                        .content(body("Public detail", "[{\"level\":\"BACHELOR\",\"name\":\"Architecture\"},{\"level\":\"MASTER\",\"name\":\"Medicine\"}]",
                                "[\"BACHELOR\",\"MASTER\"]", "PUBLISHED", null)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String slug = JsonPath.read(created, "$.view.slug");

        mvc.perform(get("/api/public/scholarships/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fields.length()").value(2))
                .andExpect(jsonPath("$.fields[1].level").value("MASTER"))
                .andExpect(jsonPath("$.fields[1].name").value("Medicine"));
    }
}
