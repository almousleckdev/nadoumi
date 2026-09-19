package com.ruoyi.nadoumi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Onboarding v2, slice 2 against the real context: the protected photo and passport
 * scan, the passport rules (valid for more than six months), the comparison with the
 * profile, and the completion gate that depends on all of it.
 */
class StudentPassportTest extends AbstractStudentIntegrationTest {

    private static final LocalDate DOB = LocalDate.now().minusYears(20);

    // ---- protected files ----

    @Test
    void shouldStoreThePhotoAndPassportScanAsProtectedFiles_andIssueSignedUrls() throws Exception {
        Student s = register("scan", "holder");

        upload(s, "/photo", "me.png").andExpect(status().isCreated()).andExpect(jsonPath("$.mediaId").isNumber());
        upload(s, "/passport/scan", "passport.png").andExpect(status().isCreated()).andExpect(jsonPath("$.mediaId").isNumber());

        mvc.perform(get(s.applicantUrl() + "/photo").param("json", "1").header("Authorization", bearer(s.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.url").isNotEmpty());
        mvc.perform(get(s.applicantUrl() + "/passport/scan").param("json", "1").header("Authorization", bearer(s.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.url").isNotEmpty());
    }

    @Test
    void shouldForbidAnotherStudent_fromUploadingOrReadingSomeoneElsesPassport() throws Exception {
        Student owner = register("owner", "passport");
        Student intruder = register("intruder", "passport");
        upload(owner, "/passport/scan", "passport.png").andExpect(status().isCreated());
        String base = owner.applicantUrl();

        upload(new Student(intruder.email(), intruder.token(), owner.applicantId()), "/passport/scan", "x.png")
                .andExpect(status().isForbidden());
        mvc.perform(get(base + "/passport/scan").param("json", "1").header("Authorization", bearer(intruder.token())))
                .andExpect(status().isForbidden());
        mvc.perform(get(base + "/passport").header("Authorization", bearer(intruder.token())))
                .andExpect(status().isForbidden());
        mvc.perform(put(base + "/passport").header("Authorization", bearer(intruder.token()))
                        .contentType(MediaType.APPLICATION_JSON).content(passportBody("MARY", "JANE", DOB, 3)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRefuseTheScanToStaffWithoutThePiiPermission() throws Exception {
        Student s = register("staff", "view");
        upload(s, "/passport/scan", "passport.png").andExpect(status().isCreated());
        createStaff("pii_less", "staff");

        mvc.perform(get("/api/staff/applicants/" + s.applicantId() + "/passport/scan").param("json", "1")
                        .header("Authorization", bearer(staffToken("pii_less"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldLetStaffWithThePiiPermissionSeeThePassportScan() throws Exception {
        Student s = register("staff", "pii");
        upload(s, "/passport/scan", "passport.png").andExpect(status().isCreated());
        createStaff("pii_full", "case_officer");

        mvc.perform(get("/api/staff/applicants/" + s.applicantId() + "/passport/scan").param("json", "1")
                        .header("Authorization", bearer(staffToken("pii_full"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.url").isNotEmpty());
    }

    // ---- passport rules ----

    @Test
    void shouldSaveAMatchingPassport_andReportItMatches() throws Exception {
        Student s = register("match", "holder");
        update(s, profile(s, DOB)).andExpect(status().isOk());

        savePassport(s, "MARY", "JANE", DOB, 3)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchesProfile").value(true))
                .andExpect(jsonPath("$.validForAdmission").value(true))
                .andExpect(jsonPath("$.passportNo").value("P1234567"))
                .andExpect(jsonPath("$.mismatches").isEmpty());
    }

    @Test
    void shouldReportEachDifferingField_whenPassportAndProfileDisagree() throws Exception {
        Student s = register("differ", "holder");
        update(s, profile(s, DOB)).andExpect(status().isOk());

        savePassport(s, "MARIE", "JANE", DOB.plusDays(1), 3)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchesProfile").value(false))
                .andExpect(jsonPath("$.mismatches[0].field").value("givenName"))
                .andExpect(jsonPath("$.mismatches[0].passportValue").value("MARIE"))
                .andExpect(jsonPath("$.mismatches[0].profileValue").value("MARY"))
                .andExpect(jsonPath("$.mismatches[1].field").value("dob"));
    }

    @Test
    void shouldRejectAPassportThatDoesNotStayValidForMoreThanSixMonths() throws Exception {
        Student s = register("expiring", "holder");
        update(s, profile(s, DOB)).andExpect(status().isOk());

        savePassport(s, "MARY", "JANE", DOB, 0).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("six months")));
        mvc.perform(get(s.applicantUrl() + "/passport").header("Authorization", bearer(s.token())))
                .andExpect(jsonPath("$.passportNo").doesNotExist());
    }

    // ---- the gate ----

    @Test
    void shouldCompleteOnboarding_onlyOncePhotoScanAndAMatchingPassportAreInPlace() throws Exception {
        Student s = register("complete", "flow");
        String complete = s.applicantUrl() + "/onboarding/complete";
        update(s, profile(s, DOB)).andExpect(status().isOk());

        mvc.perform(post(complete).header("Authorization", bearer(s.token()))).andExpect(status().isBadRequest());
        upload(s, "/photo", "me.png").andExpect(status().isCreated());
        mvc.perform(post(complete).header("Authorization", bearer(s.token()))).andExpect(status().isBadRequest());
        upload(s, "/passport/scan", "passport.png").andExpect(status().isCreated());
        savePassport(s, "MARIE", "JANE", DOB, 3).andExpect(status().isOk());
        // a passport that disagrees with the profile still blocks Finish
        mvc.perform(post(complete).header("Authorization", bearer(s.token()))).andExpect(status().isBadRequest());

        savePassport(s, "MARY", "JANE", DOB, 3).andExpect(status().isOk());

        mvc.perform(post(complete).header("Authorization", bearer(s.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.complete").value(true));
    }

    // ---- helpers ----

    private ResultActions savePassport(Student s, String given, String family, LocalDate dob, int validYears) throws Exception {
        return mvc.perform(put(s.applicantUrl() + "/passport").header("Authorization", bearer(s.token()))
                .contentType(MediaType.APPLICATION_JSON).content(passportBody(given, family, dob, validYears)));
    }

    /** {@code validYears == 0} means it expires in three months, i.e. inside the six-month window. */
    private static String passportBody(String given, String family, LocalDate dob, int validYears) {
        LocalDate expiry = validYears == 0 ? LocalDate.now().plusMonths(3) : LocalDate.now().plusYears(validYears);
        return "{\"passportNo\":\"p1234567\",\"givenName\":\"" + given + "\",\"familyName\":\"" + family
                + "\",\"dob\":\"" + dob + "\",\"issueDate\":\"" + LocalDate.now().minusYears(2)
                + "\",\"expiryDate\":\"" + expiry + "\",\"readMethod\":\"MRZ\",\"edited\":false}";
    }
}
