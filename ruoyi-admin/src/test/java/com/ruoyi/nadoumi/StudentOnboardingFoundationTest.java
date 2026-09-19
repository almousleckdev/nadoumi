package com.ruoyi.nadoumi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Onboarding v2, slice 1 against the real context: registration creates a prefilled,
 * UPPERCASE applicant; the age and email rules hold; and only the server can mark a
 * student onboarded, only for an applicant the caller may edit.
 */
class StudentOnboardingFoundationTest extends AbstractStudentIntegrationTest {

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
    void shouldRefuseCompletion_untilEverySectionIsSatisfied() throws Exception {
        Student s = register("finish", "line");
        String url = s.applicantUrl() + "/onboarding";

        mvc.perform(post(url + "/complete").header("Authorization", bearer(s.token())))
                .andExpect(status().isBadRequest());
        mvc.perform(get(url).header("Authorization", bearer(s.token())))
                .andExpect(jsonPath("$.complete").value(false))
                .andExpect(jsonPath("$.ready").value(false))
                .andExpect(jsonPath("$.sections[0].key").value("PROFILE"));

        update(s, profile(s, LocalDate.now().minusYears(20))).andExpect(status().isOk());

        // the profile alone is not enough: the photo and the passport are still outstanding
        mvc.perform(get(url).header("Authorization", bearer(s.token())))
                .andExpect(jsonPath("$.sections[0].complete").value(true))
                .andExpect(jsonPath("$.sections[1].key").value("PHOTO"))
                .andExpect(jsonPath("$.sections[1].complete").value(false))
                .andExpect(jsonPath("$.sections[2].key").value("PASSPORT"))
                .andExpect(jsonPath("$.sections[2].complete").value(false));
        mvc.perform(post(url + "/complete").header("Authorization", bearer(s.token())))
                .andExpect(status().isBadRequest());
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
}
