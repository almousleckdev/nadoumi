package com.ruoyi.nadoumi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/**
 * The generic {@code /common/*} upload/download endpoints carry no per-resource
 * permission and {@code /download?delete=true} removes files, so the controller
 * is staff-only ({@code @PreAuthorize("@currentCaller.isStaff()")}). A student
 * JWT is otherwise merely {@code authenticated()} and would pass.
 */
class CommonEndpointAuthorizationTest extends AbstractNadIntegrationTest {

    @Test
    void studentToken_cannotUseCommonUpload() throws Exception {
        createStudent("common_probe");
        String token = studentToken("common_probe");
        var file = new MockMultipartFile("file", "x.txt", "text/plain", "hi".getBytes());

        // RuoYi surface: an authorization failure is HTTP 200 + body code 403,
        // and no upload happens.
        mvc.perform(multipart("/common/upload").file(file).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.url").doesNotExist());
    }

    @Test
    void staffToken_passesTheStaffGuard() throws Exception {
        createStaff("common_staff", "ops_manager");
        String token = staffToken("common_staff");
        var file = new MockMultipartFile("file", "hello.txt", "text/plain", "hi".getBytes());

        // The guard passes; the extension policy may still reject the upload, but
        // never with code 403 from the guard.
        mvc.perform(multipart("/common/upload").file(file).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", Matchers.not(403)));
    }
}
