package com.ruoyi.nadoumi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.ruoyi.common.core.redis.RedisCache;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Full-chain proof for the student email OTP: the code is generated, stored in
 * Redis with the right TTLs, emailed, retrievable through the configured test mail
 * system ({@code LoggingMailSender} under profile {@code test}), single-use, and
 * that a resend inside the cooldown is reported as throttled without a second mail.
 */
class OtpEndToEndTest extends AbstractNadIntegrationTest {

    private static final Pattern SIX_DIGITS = Pattern.compile("\\b(\\d{6})\\b");

    @Autowired
    private RedisCache redis;

    @Test
    void otp_is_generated_stored_sent_retrievable_and_single_use() throws Exception {
        String email = "otp-e2e-" + System.nanoTime() + "@example.test";
        String otpKey = "nad:otp:REGISTER:" + sha256(email.toLowerCase());
        String cooldownKey = "nad:otp:cooldown:REGISTER:" + sha256(email.toLowerCase());

        // 1. request a code
        mvc.perform(post("/api/student/email-otp").contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"purpose\":\"REGISTER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sent").value(true))
                .andExpect(jsonPath("$.throttled").value(false))
                .andExpect(jsonPath("$.retryAfter").value(60));

        // 2. stored in Redis with the expected TTLs
        assertThat(redis.getExpire(otpKey)).isBetween(1L, 600L);
        assertThat(redis.getExpire(cooldownKey)).isBetween(1L, 60L);

        // 3. retrievable through the test mail system, with a 6-digit code in the body
        String mail = mvc.perform(get("/api/dev/mail/latest").param("to", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("Verify your email"))
                .andReturn().getResponse().getContentAsString();
        String code = extractCode(JsonPath.read(mail, "$.body"));

        // 4. an immediate resend is throttled and sends no second mail
        mvc.perform(post("/api/student/email-otp").contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"purpose\":\"REGISTER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sent").value(true))
                .andExpect(jsonPath("$.throttled").value(true))
                .andExpect(jsonPath("$.retryAfter", org.hamcrest.Matchers.greaterThan(0)));
        String mailAfterResend = mvc.perform(get("/api/dev/mail/latest").param("to", email))
                .andReturn().getResponse().getContentAsString();
        assertThat(extractCode(JsonPath.read(mailAfterResend, "$.body"))).isEqualTo(code);

        // 5. the code verifies once, returns a ticket, and is burned
        String verify = mvc.perform(post("/api/student/email-otp/verify").contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"purpose\":\"REGISTER\",\"otp\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.read(verify, "$.ticket").toString()).isNotBlank();
        assertThat(redis.getExpire(otpKey)).isNegative(); // -2: key gone

        // 6. the same code cannot be reused
        mvc.perform(post("/api/student/email-otp/verify").contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"purpose\":\"REGISTER\",\"otp\":\"" + code + "\"}"))
                .andExpect(status().isBadRequest());
    }

    private static String extractCode(String body) {
        Matcher m = SIX_DIGITS.matcher(body);
        assertThat(m.find()).as("6-digit code in mail body").isTrue();
        return m.group(1);
    }

    private static String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
