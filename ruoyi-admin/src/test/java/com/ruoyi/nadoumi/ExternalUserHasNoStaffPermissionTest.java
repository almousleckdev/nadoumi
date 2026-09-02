package com.ruoyi.nadoumi;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** CI-blocking: an external token fails every nad:* / system:* RBAC check by construction. */
class ExternalUserHasNoStaffPermissionTest extends AbstractNadIntegrationTest {

    @Test
    void studentTokenIsRejectedByStaffEndpoints() throws Exception {
        createStudent("no_staff_perm");
        String token = studentToken("no_staff_perm");

        mvc.perform(get("/api/staff/applicants").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/staff/applicants").header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content("{\"givenName\":\"X\",\"familyName\":\"Y\",\"invitedEmail\":\"z@example.test\"}"))
                .andExpect(status().isForbidden());

        // RuoYi's own endpoints keep the AjaxResult envelope (HTTP 200, body code 403)
        mvc.perform(get("/system/user/list").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }
}
