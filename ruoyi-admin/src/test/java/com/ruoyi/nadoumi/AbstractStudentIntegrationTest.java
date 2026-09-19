package com.ruoyi.nadoumi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Helpers for tests that drive a real student through registration and onboarding:
 * register (OTP included), log in, fill the profile, upload the protected files.
 */
abstract class AbstractStudentIntegrationTest extends AbstractNadIntegrationTest {

    /** Registration enforces the real password policy (unlike the fixture helpers). */
    protected static final String STRONG_PASSWORD = "Onboard-Test-9!x";

    private static final Pattern SIX_DIGITS = Pattern.compile("\\b(\\d{6})\\b");
    private static final AtomicInteger SEQ = new AtomicInteger();
    private static final byte[] PNG = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4, (byte) 0x89
    };

    protected record Student(String email, String token, long applicantId) {
        String applicantUrl() {
            return "/api/student/applicants/" + applicantId;
        }
    }

    protected ResultActions update(Student s, String body) throws Exception {
        return mvc.perform(put(s.applicantUrl()).header("Authorization", bearer(s.token()))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** A complete, valid profile body for {@code s}. */
    protected static String profile(Student s, LocalDate dob) {
        return "{\"givenName\":\"MARY\",\"familyName\":\"JANE\",\"dob\":\"" + dob + "\",\"nationality\":\"eg\","
                + "\"email\":\"" + s.email() + "\",\"phone\":\"+8613800000000\",\"gender\":\"FEMALE\","
                + "\"countryOfOrigin\":\"eg\",\"countryOfResidence\":\"cn\",\"nativeLanguage\":\"AR\","
                + "\"whatsapp\":\"+8613800000000\"}";
    }

    protected Student register(String first, String last) throws Exception {
        String email = uniqueEmail();
        String ticket = ticketFor(email);
        mvc.perform(post("/api/student/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(first, last, email, ticket)))
                .andExpect(status().isCreated());
        String login = mvc.perform(post("/api/student/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + STRONG_PASSWORD + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(login, "$.token");
        String mine = mvc.perform(get("/api/student/applicants").header("Authorization", bearer(token)))
                .andReturn().getResponse().getContentAsString();
        return new Student(email, token, ((Number) JsonPath.read(mine, "$[0].id")).longValue());
    }

    protected ResultActions upload(Student s, String path, String filename) throws Exception {
        return mvc.perform(multipart(s.applicantUrl() + path)
                .file(new MockMultipartFile("file", filename, "image/png", PNG))
                .header("Authorization", bearer(s.token())));
    }

    /** Education, interests, location and a guardian: every section except profile, photo and passport. */
    protected void fillProfileSections(Student s) throws Exception {
        String auth = bearer(s.token());
        mvc.perform(post(s.applicantUrl() + "/education").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"institution\":\"Cairo High School\",\"country\":\"EG\",\"level\":\"HIGH_SCHOOL\","
                                + "\"startDate\":\"2018-09-01\",\"endDate\":\"2021-06-30\"}"))
                .andExpect(status().isCreated());
        mvc.perform(put(s.applicantUrl() + "/interests").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"desiredLevel\":\"MASTER\",\"fields\":[\"BUSINESS\"],\"cities\":[\"Beijing\"]}"))
                .andExpect(status().isOk());
        mvc.perform(put(s.applicantUrl() + "/residence").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inChina\":false,\"country\":\"EG\",\"city\":\"Cairo\"}"))
                .andExpect(status().isOk());
        mvc.perform(post(s.applicantUrl() + "/contacts").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"relation\":\"GUARDIAN\",\"name\":\"Parent One\",\"phone\":\"+201000000\"}"))
                .andExpect(status().isCreated());
    }

    protected static String uniqueEmail() {
        return "stu" + SEQ.incrementAndGet() + "-" + System.nanoTime() + "@example.test";
    }

    protected String latestCode(String email, String subject) throws Exception {
        String mail = mvc.perform(get("/api/dev/mail/latest").param("to", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value(subject))
                .andReturn().getResponse().getContentAsString();
        Matcher m = SIX_DIGITS.matcher(JsonPath.<String>read(mail, "$.body"));
        assertThat(m.find()).as("6-digit code in mail body").isTrue();
        return m.group(1);
    }

    protected static String registerBody(String first, String last, String email, String ticket) {
        return "{\"firstName\":\"" + first + "\",\"lastName\":\"" + last + "\",\"email\":\"" + email
                + "\",\"password\":\"" + STRONG_PASSWORD + "\",\"ticket\":\"" + ticket + "\"}";
    }

    protected String ticketFor(String email) throws Exception {
        mvc.perform(post("/api/student/email-otp").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"purpose\":\"REGISTER\"}"))
                .andExpect(status().isOk());
        String code = latestCode(email, "Verify your email — Nadoumi");
        String res = mvc.perform(post("/api/student/email-otp/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"purpose\":\"REGISTER\",\"otp\":\"" + code + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(res, "$.ticket");
    }
}
