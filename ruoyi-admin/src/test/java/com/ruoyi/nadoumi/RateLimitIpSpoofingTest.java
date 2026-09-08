package com.ruoyi.nadoumi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/**
 * A {@code limitType = IP} {@code @RateLimiter} must key on the socket peer, not
 * a client-supplied {@code X-Forwarded-For} — otherwise an attacker rotates the
 * header to get a fresh bucket on every request. {@code AbstractNadIntegrationTest}
 * clears the rate-limit keys before each test.
 */
class RateLimitIpSpoofingTest extends AbstractNadIntegrationTest {

    @Test
    void rotatingForwardedForCannotResetTheIpBucket() throws Exception {
        String body = "{\"email\":\"ip-spoof@example.test\",\"purpose\":\"REGISTER\"}";

        // POST /api/student/email-otp is @RateLimiter(count = 20, time = 3600,
        // limitType = IP). All 21 calls share one socket peer (127.0.0.1) despite
        // a different X-Forwarded-For each time, so the 21st still trips the limit.
        for (int i = 0; i < 20; i++) {
            mvc.perform(post("/api/student/email-otp").contentType("application/json")
                    .header("X-Forwarded-For", "203.0.113." + i)
                    .content(body));
        }

        mvc.perform(post("/api/student/email-otp").contentType("application/json")
                        .header("X-Forwarded-For", "203.0.113.240")
                        .content(body))
                .andExpect(status().isTooManyRequests());
    }
}
