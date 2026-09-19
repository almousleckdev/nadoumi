package com.ruoyi.nadoumi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Onboarding v2, slice 1 against the real context: registration creates a prefilled,
 * UPPERCASE applicant; the age and email rules hold; and only the server can mark a
 * student onboarded, only for an applicant the caller may edit.
 */
class StudentOnboardingFoundationTest extends AbstractNadIntegrationTest {

    private static final Pattern SIX_DIGITS = Pattern.compile("\\b(\\d{6})\\b");
    private static final AtomicInteger SEQ = new AtomicInteger();
    /** Registration enforces the real password policy (unlike the fixture helpers). */
    private static final String STRONG_PASSWORD = "Onboard-Test-9!x";

    private record Student(String email, String token, long applicantId) {
    }

    // ---- registration ----

    @Test
    void shouldCreatePrefilledUppercaseApplicant_whenStudentRegisters() throws Exception {
        Student s = register("ahmed", "hassan");

        mvc.perform(get("/api/student/applicants/" + s.applicantId()).header("Authorization", bearer(s.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.givenName").value("AHMED"))
                .andExpect(jsonPath("$.familyName").value("HASSAN"))
                .andExpect(jsonPath("$.email").value(s.email()))
                .andExpect(jsonPath("$.emailVerified").value(true))
                .andExpect(jsonPath("$.onboardingComplete").value(false));
    }

    @Test
    void shouldRejectRegistration_whenNameContainsDigits() throws Exception {
        String email = uniqueEmail();
        String ticket = ticketFor(email);

        mvc.perform(post("/api/student/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("john2", "doe", email, ticket)))
                .andExpect(status().isBadRequest());
    }

    // ---- profile rules ----

    @Test
    void shouldStoreUppercaseNamesAndCountries_whenProfileIsSaved() throws Exception {
        Student s = register("mary", "jane");

        update(s, profile(s, LocalDate.now().minusYears(20)).replace("\"givenName\":\"MARY\"", "\"givenName\":\" mary  ann \""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.givenName").value("MARY ANN"))
                .andExpect(jsonPath("$.countryOfResidence").value("CN"))
                .andExpect(jsonPath("$.nativeLanguage").value("ar"));
    }

    @Test
    void shouldRejectProfile_whenDobIsUnderSeventeenOrInTheFuture() throws Exception {
        Student s = register("young", "student");

        update(s, profile(s, LocalDate.now().minusYears(16))).andExpect(status().isBadRequest());
        update(s, profile(s, LocalDate.now().plusDays(1))).andExpect(status().isBadRequest());
        update(s, profile(s, LocalDate.now().minusYears(17))).andExpect(status().isOk());
    }

    @Test
    void shouldRejectProfile_whenEmailChangedWithoutVerification() throws Exception {
        Student s = register("email", "changer");
        String body = profile(s, LocalDate.now().minusYears(20)).replace(s.email(), "other-" + s.email());

        update(s, body).andExpect(status().isBadRequest());
    }

    // ---- email verification ----

    @Test
    void shouldVerifyAndStoreNewEmail_whenCodeIsCorrect() throws Exception {
        Student s = register("verify", "email");
        String newEmail = uniqueEmail();

        mvc.perform(post("/api/student/applicants/" + s.applicantId() + "/email/otp")
                        .header("Authorization", bearer(s.token())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + newEmail + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sent").value(true));
        String code = latestCode(newEmail, "Confirm your email — Nadoumi");

        mvc.perform(post("/api/student/applicants/" + s.applicantId() + "/email/verify")
                        .header("Authorization", bearer(s.token())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + newEmail + "\",\"otp\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(newEmail))
                .andExpect(jsonPath("$.emailVerified").value(true));
    }

    @Test
    void shouldRefuseApplicantEmailCode_onTheAnonymousOtpEndpoints() throws Exception {
        mvc.perform(post("/api/student/email-otp").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + uniqueEmail() + "\",\"purpose\":\"APPLICANT_EMAIL\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---- the gate ----

    @Test
    void shouldRefuseCompletion_untilTheProfileIsComplete_thenRecordIt() throws Exception {
        Student s = register("finish", "line");
        String url = "/api/student/applicants/" + s.applicantId() + "/onboarding";

        mvc.perform(post(url + "/complete").header("Authorization", bearer(s.token())))
                .andExpect(status().isBadRequest());
        mvc.perform(get(url).header("Authorization", bearer(s.token())))
                .andExpect(jsonPath("$.complete").value(false))
                .andExpect(jsonPath("$.ready").value(false))
                .andExpect(jsonPath("$.sections[0].key").value("PROFILE"));

        update(s, profile(s, LocalDate.now().minusYears(20))).andExpect(status().isOk());

        mvc.perform(post(url + "/complete").header("Authorization", bearer(s.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complete").value(true));
        mvc.perform(get("/api/student/applicants/" + s.applicantId()).header("Authorization", bearer(s.token())))
                .andExpect(jsonPath("$.onboardingComplete").value(true));
    }

    @Test
    void shouldForbidAnotherStudent_fromCompletingOrVerifyingSomeoneElsesApplicant() throws Exception {
        Student owner = register("owner", "one");
        Student intruder = register("intruder", "two");
        String base = "/api/student/applicants/" + owner.applicantId();

        mvc.perform(post(base + "/onboarding/complete").header("Authorization", bearer(intruder.token())))
                .andExpect(status().isForbidden());
        mvc.perform(get(base + "/onboarding").header("Authorization", bearer(intruder.token())))
                .andExpect(status().isForbidden());
        mvc.perform(post(base + "/email/otp").header("Authorization", bearer(intruder.token()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + uniqueEmail() + "\"}"))
                .andExpect(status().isForbidden());
    }

    // ---- helpers ----

    private ResultActions update(Student s, String body) throws Exception {
        return mvc.perform(put("/api/student/applicants/" + s.applicantId())
                .header("Authorization", bearer(s.token())).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String profile(Student s, LocalDate dob) {
        return "{\"givenName\":\"MARY\",\"familyName\":\"JANE\",\"dob\":\"" + dob + "\",\"nationality\":\"eg\","
                + "\"email\":\"" + s.email() + "\",\"phone\":\"+8613800000000\",\"gender\":\"FEMALE\","
                + "\"countryOfOrigin\":\"eg\",\"countryOfResidence\":\"cn\",\"nativeLanguage\":\"AR\","
                + "\"whatsapp\":\"+8613800000000\"}";
    }

    private Student register(String first, String last) throws Exception {
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

    private static String registerBody(String first, String last, String email, String ticket) {
        return "{\"firstName\":\"" + first + "\",\"lastName\":\"" + last + "\",\"email\":\"" + email
                + "\",\"password\":\"" + STRONG_PASSWORD + "\",\"ticket\":\"" + ticket + "\"}";
    }

    private String ticketFor(String email) throws Exception {
        mvc.perform(post("/api/student/email-otp").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"purpose\":\"REGISTER\"}"))
                .andExpect(status().isOk());
        String code = latestCode(email, "Verify your email — Nadoumi");
        String res = mvc.perform(post("/api/student/email-otp/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"purpose\":\"REGISTER\",\"otp\":\"" + code + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(res, "$.ticket");
    }

    private String latestCode(String email, String subject) throws Exception {
        String mail = mvc.perform(get("/api/dev/mail/latest").param("to", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value(subject))
                .andReturn().getResponse().getContentAsString();
        Matcher m = SIX_DIGITS.matcher(JsonPath.<String>read(mail, "$.body"));
        assertThat(m.find()).as("6-digit code in mail body").isTrue();
        return m.group(1);
    }

    private static String uniqueEmail() {
        return "onb" + SEQ.incrementAndGet() + "-" + System.nanoTime() + "@example.test";
    }
}
