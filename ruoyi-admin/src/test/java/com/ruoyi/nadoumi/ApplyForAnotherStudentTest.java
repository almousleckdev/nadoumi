package com.ruoyi.nadoumi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** A user applying for someone else gets that person's own applicant record, and nobody else can reach it. */
class ApplyForAnotherStudentTest extends AbstractStudentIntegrationTest {

    private static final String OTHER = "{\"givenName\":\"Aminata\",\"familyName\":\"Diallo\",\"nationality\":\"gn\","
            + "\"countryOfOrigin\":\"gn\",\"gender\":\"FEMALE\"}";

    private long createOther(Student agent) throws Exception {
        String res = mvc.perform(post("/api/student/applicants/other").header("Authorization", bearer(agent.token()))
                        .contentType(MediaType.APPLICATION_JSON).content(OTHER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.givenName").value("AMINATA")) // names are kept as in a passport
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(res, "$.id")).longValue();
    }

    @Test
    void shouldGiveTheAgentTheirOwnStudentAlongsideThemselves_andLetThemEditIt() throws Exception {
        Student agent = register("agent", "one");
        long other = createOther(agent);

        mvc.perform(get("/api/student/applicants").header("Authorization", bearer(agent.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(put("/api/student/applicants/" + other).header("Authorization", bearer(agent.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OTHER.replace("Aminata", "Aminata B")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.givenName").value("AMINATA B"));
    }

    @Test
    void shouldKeepTheNewStudentAwayFromEveryoneElse() throws Exception {
        Student agent = register("agent", "two");
        Student stranger = register("agent", "three");
        long other = createOther(agent);

        mvc.perform(get("/api/student/applicants/" + other).header("Authorization", bearer(stranger.token())))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/student/applicants").header("Authorization", bearer(stranger.token())))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldRefuseAnApplicantWithoutAName() throws Exception {
        Student agent = register("agent", "four");

        mvc.perform(post("/api/student/applicants/other").header("Authorization", bearer(agent.token()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"givenName\":\"\",\"familyName\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
