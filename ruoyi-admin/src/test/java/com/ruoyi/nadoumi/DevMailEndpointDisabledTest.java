package com.ruoyi.nadoumi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Security regression guard: with {@code nadoumi.mail.dev-inbox.enabled=false}
 * (the default everywhere except the ITs and local E2E), the anonymous
 * {@code GET /api/dev/mail/latest} must not be mounted, so an unauthenticated
 * caller cannot read a recipient's OTP / reset codes.
 */
@TestPropertySource(properties = "nadoumi.mail.dev-inbox.enabled=false")
class DevMailEndpointDisabledTest extends AbstractNadIntegrationTest {

    @Test
    void devMailInbox_isNotReachable_whenFlagDisabled() throws Exception {
        MvcResult result = mvc.perform(get("/api/dev/mail/latest").param("to", "victim@example.com"))
                .andReturn();

        int status = result.getResponse().getStatus();
        String body = result.getResponse().getContentAsString();

        // No handler is registered, so the request never reaches DevMailController.
        // It is either rejected (RuoYi's entry point renders a {"code":401} envelope
        // with HTTP 200, or a bare 401/403) or 404 — never a serialised EmailMessage.
        assertThat(status).isIn(200, 401, 403, 404);
        assertThat(body)
                .doesNotContain("victim@example.com")
                .doesNotContain("\"subject\"")
                .doesNotContain("\"body\"");
    }
}
