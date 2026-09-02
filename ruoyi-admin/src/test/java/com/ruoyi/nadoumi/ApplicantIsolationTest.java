package com.ruoyi.nadoumi;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** CI-blocking: one student cannot see or list another student's applicant. */
class ApplicantIsolationTest extends AbstractNadIntegrationTest {

    private static final String PROFILE = """
            {"givenName":"A","familyName":"B","nationality":"MR"}""";

    @Test
    void studentCannotReadAnotherStudentsApplicant() throws Exception {
        createStudent("iso_a");
        createStudent("iso_b");
        String tokenA = studentToken("iso_a");
        String tokenB = studentToken("iso_b");

        String created = mvc.perform(post("/api/student/applicants")
                        .header("Authorization", bearer(tokenA))
                        .contentType("application/json").content(PROFILE))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long applicantA = ((Number) JsonPath.read(created, "$.id")).longValue();

        mvc.perform(get("/api/student/applicants/{id}", applicantA).header("Authorization", bearer(tokenB)))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/student/applicants/{id}", applicantA).header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk());
    }

    @Test
    void studentListReturnsOnlyOwnApplicants() throws Exception {
        createStudent("list_a");
        createStudent("list_b");
        String tokenA = studentToken("list_a");
        String tokenB = studentToken("list_b");

        mvc.perform(post("/api/student/applicants").header("Authorization", bearer(tokenA))
                .contentType("application/json").content(PROFILE)).andExpect(status().isCreated());
        mvc.perform(post("/api/student/applicants").header("Authorization", bearer(tokenB))
                .contentType("application/json").content(PROFILE)).andExpect(status().isCreated());

        mvc.perform(get("/api/student/applicants").header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }
}
