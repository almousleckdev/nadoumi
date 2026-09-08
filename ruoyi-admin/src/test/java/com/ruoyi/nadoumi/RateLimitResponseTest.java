package com.ruoyi.nadoumi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/**
 * The {@code @RateLimiter} aspect must answer an exceeded quota with HTTP 429 +
 * {@code Retry-After} (API gateways and fetch clients depend on it), not RuoYi's
 * HTTP 200 {@code AjaxResult} envelope. {@code AbstractNadIntegrationTest} clears
 * the rate-limit keys before each test, so the counter starts fresh here.
 */
class RateLimitResponseTest extends AbstractNadIntegrationTest {

    @Test
    void exceededQuota_returns429WithRetryAfter() throws Exception {
        String body = "{\"email\":\"ratelimit-probe@example.com\",\"password\":\"whatever\"}";

        // POST /api/student/login is @RateLimiter(count = 10, time = 60): the 11th
        // call inside the window trips the limiter (before the controller runs).
        for (int i = 0; i < 10; i++) {
            mvc.perform(post("/api/student/login").contentType("application/json").content(body));
        }

        mvc.perform(post("/api/student/login").contentType("application/json").content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.status").value(429));
    }
}
