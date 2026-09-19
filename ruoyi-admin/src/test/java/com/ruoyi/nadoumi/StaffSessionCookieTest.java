package com.ruoyi.nadoumi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * The admin console keeps its session in an httpOnly cookie so script (an XSS) can
 * never read the token. Cookie credentials are honoured on unsafe methods only when
 * the request carries the custom client header, which a cross-site form or
 * simple request cannot set.
 */
class StaffSessionCookieTest extends AbstractNadIntegrationTest {

    private static final String COOKIE = "NAD_ADMIN_SESSION";
    private static final String CLIENT_HEADER = "X-Nadoumi-Client";
    private static final String CLIENT_VALUE = "admin";

    @Test
    void shouldSetHttpOnlyStrictCookieAndHideTokenFromBody_whenStaffSignsIn() throws Exception {
        createStaff("cookie_staff", "ops_manager");

        MvcResult res = signIn("cookie_staff", PASSWORD);

        String setCookie = res.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).startsWith(COOKIE + "=").contains("HttpOnly", "SameSite=Strict", "Path=/", "Secure");
        assertThat(res.getResponse().getContentAsString()).doesNotContain("token");
    }

    @Test
    void shouldRejectSignIn_whenPasswordIsWrong() throws Exception {
        createStaff("cookie_wrong", "ops_manager");

        MvcResult res = signIn("cookie_wrong", "not-the-password");

        assertThat(res.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();
    }

    @Test
    void shouldAuthenticateSafeRequest_whenOnlyTheCookieIsPresent() throws Exception {
        createStaff("cookie_get", "ops_manager");
        Cookie session = sessionCookie(signIn("cookie_get", PASSWORD));

        mvc.perform(get("/getInfo").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.user.userName").value("cookie_get"));
    }

    @Test
    void shouldIgnoreCookieOnUnsafeRequest_whenClientHeaderIsMissing() throws Exception {
        createStaff("cookie_csrf", "ops_manager");
        Cookie session = sessionCookie(signIn("cookie_csrf", PASSWORD));

        mvc.perform(put("/system/user/profile").cookie(session)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldAuthenticateUnsafeRequest_whenClientHeaderIsPresent() throws Exception {
        createStaff("cookie_write", "ops_manager");
        Cookie session = sessionCookie(signIn("cookie_write", PASSWORD));

        MvcResult res = mvc.perform(put("/system/user/profile").cookie(session)
                        .header(CLIENT_HEADER, CLIENT_VALUE)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn();

        assertThat(res.getResponse().getContentAsString()).doesNotContain("\"code\":401");
    }

    @Test
    void shouldEndSessionAndClearCookie_whenStaffSignsOut() throws Exception {
        createStaff("cookie_out", "ops_manager");
        Cookie session = sessionCookie(signIn("cookie_out", PASSWORD));

        MvcResult res = mvc.perform(post("/logout").cookie(session).header(CLIENT_HEADER, CLIENT_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        List<String> cleared = res.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(cleared).anySatisfy(c -> assertThat(c).startsWith(COOKIE + "=;").contains("Max-Age=0"));
        mvc.perform(get("/getInfo").cookie(session)).andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldStillAcceptBearerHeader_whenNoCookieIsSent() throws Exception {
        createStaff("cookie_bearer", "ops_manager");
        String token = staffToken("cookie_bearer");

        mvc.perform(get("/getInfo").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.code").value(200));
    }

    private MvcResult signIn(String username, String password) throws Exception {
        return mvc.perform(post("/staff/session").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andReturn();
    }

    private static Cookie sessionCookie(MvcResult signIn) {
        String setCookie = signIn.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).as("Set-Cookie on sign-in").isNotNull();
        String value = setCookie.substring((COOKIE + "=").length(), setCookie.indexOf(';'));
        return new Cookie(COOKIE, value);
    }
}
