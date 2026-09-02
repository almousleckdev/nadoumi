package com.ruoyi.nadoumi;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CI-blocking: a revoked grant denies the very next request from the same token,
 * with no re-login — grants are not cached into the Redis LoginUser.
 */
class RevocationTakesEffectNextRequestTest extends AbstractNadIntegrationTest {

    @Test
    void revokedViewerLosesAccessOnNextCall() throws Exception {
        createStudent("rev_owner");
        long viewerUserId = createStudent("rev_viewer");
        String ownerToken = studentToken("rev_owner");
        String viewerToken = studentToken("rev_viewer");

        String created = mvc.perform(post("/api/student/applicants").header("Authorization", bearer(ownerToken))
                        .contentType("application/json")
                        .content("{\"givenName\":\"P\",\"familyName\":\"Q\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long applicantId = ((Number) JsonPath.read(created, "$.id")).longValue();

        mvc.perform(post("/api/student/applicants/{id}/access", applicantId)
                        .header("Authorization", bearer(ownerToken)).contentType("application/json")
                        .content("{\"userId\":" + viewerUserId + ",\"role\":\"VIEWER\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/student/applicants/{id}", applicantId).header("Authorization", bearer(viewerToken)))
                .andExpect(status().isOk());

        String grants = mvc.perform(get("/api/student/applicants/{id}/access", applicantId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Integer> viewerGrantIds = JsonPath.read(grants, "$[?(@.accessRole == 'VIEWER')].id");
        int grantId = viewerGrantIds.get(0);

        mvc.perform(delete("/api/student/applicants/{id}/access/{grantId}", applicantId, grantId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/student/applicants/{id}", applicantId).header("Authorization", bearer(viewerToken)))
                .andExpect(status().isForbidden());
    }
}
