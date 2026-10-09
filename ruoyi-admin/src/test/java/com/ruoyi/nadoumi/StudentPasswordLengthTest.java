package com.ruoyi.nadoumi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * The student password policy allows 8 to 32 characters, so a student who picks a long one must be able to sign
 * in with it. RuoYi's own login pre-check used to cap passwords at 20, which left such a student registered but
 * unable to sign in.
 */
class StudentPasswordLengthTest extends AbstractStudentIntegrationTest {

    private static final String LONG_PASSWORD = "Aa1!" + "x".repeat(28); // 32 characters, the policy maximum

    private void register(String email, String username, String password) throws Exception {
        String ticket = ticketFor(email);
        mvc.perform(post("/api/student/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"email\":\"" + email + "\",\"password\":\""
                                + password + "\",\"ticket\":\"" + ticket + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldSignInRightAfterRegistering_whenThePasswordIsAtTheMaximumLength() throws Exception {
        String email = uniqueEmail();
        register(email, usernameFor("long", "pass"), LONG_PASSWORD);

        String body = mvc.perform(post("/api/student/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + LONG_PASSWORD + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(body).as("login response").contains("\"token\"");
    }

    @Test
    void shouldRejectAPasswordOverTheMaximum_beforeAnyAccountExists() throws Exception {
        String email = uniqueEmail();
        String ticket = ticketFor(email);
        String tooLong = LONG_PASSWORD + "y";

        mvc.perform(post("/api/student/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + usernameFor("too", "long") + "\",\"email\":\"" + email
                                + "\",\"password\":\"" + tooLong + "\",\"ticket\":\"" + ticket + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("password.tooLong"));

        assertThat(jdbc.queryForObject("select count(*) from sys_user where email = ?", Integer.class, email)).isZero();
    }

    @Test
    void shouldNameThePasswordRule_whenThePasswordContainsTheUsername() throws Exception {
        String email = uniqueEmail();
        String ticket = ticketFor(email);

        mvc.perform(post("/api/student/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"amina_k\",\"email\":\"" + email
                                + "\",\"password\":\"Amina_k-Pass9!\",\"ticket\":\"" + ticket + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("password.noPersonal"));

        assertThat(jdbc.queryForObject("select count(*) from sys_user where email = ?", Integer.class, email)).isZero();
    }
}
