package com.ruoyi.nadoumi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
 * Onboarding v2 slice 3 against the real context: education history, study interests, current
 * location (with the China branch), guardian contact and work experience, plus the completion
 * gate and the once-only welcome record that depend on them.
 */
class StudentProfileSectionsTest extends AbstractStudentIntegrationTest {

    // ---- education ----

    @Test
    void shouldKeepSeveralEducationRecords_fromHighSchoolToTheCurrentLevel() throws Exception {
        Student s = register("edu", "many");

        education(s, "Cairo High School", "HIGH_SCHOOL", "2018-09-01", "2021-06-30", false).andExpect(status().isCreated());
        education(s, "Cairo University", "BACHELOR", "2021-09-01", null, true).andExpect(status().isCreated());

        mvc.perform(get(s.applicantUrl() + "/education").header("Authorization", bearer(s.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].current").value(true));
    }

    @Test
    void shouldRejectAnEducationRecordWithAnUnknownLevelOrBadDates() throws Exception {
        Student s = register("edu", "bad");

        education(s, "School", "PRIMARY", "2018-09-01", "2021-06-30", false).andExpect(status().isBadRequest());
        education(s, "School", "HIGH_SCHOOL", "2021-09-01", "2018-06-30", false).andExpect(status().isBadRequest());
    }

    // ---- interests ----

    @Test
    void shouldSaveInterestsAndReplaceTheChoicesOnEachSave() throws Exception {
        Student s = register("int", "erest");
        mvc.perform(get(s.applicantUrl() + "/interests").header("Authorization", bearer(s.token())))
                .andExpect(status().isNoContent());

        interests(s, "[\"BUSINESS\",\"LAW\"]", "[\"Beijing\",\"Shanghai\"]").andExpect(status().isOk());
        interests(s, "[\"LAW\"]", "[\"Hangzhou\"]").andExpect(status().isOk());

        mvc.perform(get(s.applicantUrl() + "/interests").header("Authorization", bearer(s.token())))
                .andExpect(jsonPath("$.desiredLevel").value("MASTER"))
                .andExpect(jsonPath("$.fields.length()").value(1))
                .andExpect(jsonPath("$.cities[0]").value("Hangzhou"));
    }

    @Test
    void shouldRequireAtLeastOneFieldAndOneCity() throws Exception {
        Student s = register("int", "empty");

        interests(s, "[]", "[\"Beijing\"]").andExpect(status().isBadRequest());
        interests(s, "[\"LAW\"]", "[]").andExpect(status().isBadRequest());
    }

    // ---- residence ----

    @Test
    void shouldTakeTheChinaDetailsOnlyForAStudentInChina() throws Exception {
        Student s = register("loc", "china");
        String visa = LocalDate.now().plusMonths(8).toString();

        residence(s, "{\"inChina\":true,\"country\":\"CN\",\"city\":\"Beijing\",\"chinaEducationLevel\":\"BACHELOR\","
                + "\"chinaSchool\":\"Peking University\",\"visaType\":\"X1\",\"visaExpiryDate\":\"" + visa + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visaType").value("X1"));
        residence(s, "{\"inChina\":false,\"country\":\"EG\",\"city\":\"Cairo\",\"visaType\":\"X1\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visaType").doesNotExist());
    }

    @Test
    void shouldRefuseAStudentInChinaWithoutAValidVisa() throws Exception {
        Student s = register("loc", "nochina");
        String expired = LocalDate.now().minusDays(1).toString();

        residence(s, "{\"inChina\":true,\"country\":\"CN\",\"city\":\"Beijing\",\"chinaEducationLevel\":\"BACHELOR\","
                + "\"visaType\":\"X1\",\"visaExpiryDate\":\"" + expired + "\"}").andExpect(status().isBadRequest());
        residence(s, "{\"inChina\":true,\"country\":\"CN\",\"city\":\"Beijing\"}").andExpect(status().isBadRequest());
    }

    // ---- work experience ----

    @Test
    void shouldStoreWorkExperienceAndRequireTheWorkVisaForWorkInChina() throws Exception {
        Student s = register("work", "er");

        work(s, "{\"employer\":\"Acme\",\"jobTitle\":\"Teacher\",\"country\":\"EG\",\"startDate\":\"2023-01-01\",\"current\":true}")
                .andExpect(status().isCreated());
        work(s, "{\"employer\":\"Beijing School\",\"jobTitle\":\"Teacher\",\"country\":\"CN\",\"startDate\":\"2023-01-01\","
                + "\"current\":true}").andExpect(status().isBadRequest());
        work(s, "{\"employer\":\"Beijing School\",\"jobTitle\":\"Teacher\",\"country\":\"CN\",\"startDate\":\"2023-01-01\","
                + "\"current\":true,\"workVisaType\":\"Z\"}").andExpect(status().isCreated());

        mvc.perform(get(s.applicantUrl() + "/work").header("Authorization", bearer(s.token())))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldDeleteOnlyTheStudentsOwnWork() throws Exception {
        Student owner = register("work", "owner");
        Student other = register("work", "other");
        String created = work(owner, "{\"employer\":\"Acme\",\"jobTitle\":\"Teacher\",\"country\":\"EG\","
                + "\"startDate\":\"2023-01-01\",\"current\":true}").andReturn().getResponse().getContentAsString();
        long workId = ((Number) com.jayway.jsonpath.JsonPath.read(created, "$.id")).longValue();

        mvc.perform(delete(owner.applicantUrl() + "/work/" + workId).header("Authorization", bearer(other.token())))
                .andExpect(status().isForbidden());
        mvc.perform(delete(owner.applicantUrl() + "/work/" + workId).header("Authorization", bearer(owner.token())))
                .andExpect(status().isNoContent());
    }

    // ---- gate and welcome ----

    @Test
    void shouldNotCountGuardiansWithoutAPhoneTowardsTheContactSection() throws Exception {
        Student s = register("guard", "ian");
        mvc.perform(post(s.applicantUrl() + "/contacts").header("Authorization", bearer(s.token()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"relation\":\"GUARDIAN\",\"name\":\"No Phone\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get(s.applicantUrl() + "/onboarding").header("Authorization", bearer(s.token())))
                .andExpect(jsonPath("$.sections[?(@.key=='CONTACT')].complete").value(false));
    }

    @Test
    void shouldShowTheWelcomeOnceAfterOnboardingIsComplete() throws Exception {
        Student s = register("wel", "come");
        update(s, profile(s, LocalDate.now().minusYears(20))).andExpect(status().isOk());
        fillProfileSections(s);
        upload(s, "/photo", "me.png").andExpect(status().isCreated());
        upload(s, "/passport/scan", "passport.png").andExpect(status().isCreated());
        mvc.perform(put(s.applicantUrl() + "/passport").header("Authorization", bearer(s.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passportNo\":\"p1234567\",\"givenName\":\"MARY\",\"familyName\":\"JANE\",\"dob\":\""
                                + LocalDate.now().minusYears(20) + "\",\"issueDate\":\"" + LocalDate.now().minusYears(2)
                                + "\",\"expiryDate\":\"" + LocalDate.now().plusYears(5) + "\",\"readMethod\":\"MANUAL\",\"edited\":false}"))
                .andExpect(status().isOk());
        String base = s.applicantUrl();
        String auth = bearer(s.token());

        mvc.perform(post(base + "/onboarding/welcomed").header("Authorization", auth)).andExpect(status().isNoContent());
        mvc.perform(get(base).header("Authorization", auth)).andExpect(jsonPath("$.welcomePending").value(false));

        mvc.perform(post(base + "/onboarding/complete").header("Authorization", auth)).andExpect(status().isOk());
        mvc.perform(get(base).header("Authorization", auth)).andExpect(jsonPath("$.welcomePending").value(true));
        mvc.perform(post(base + "/onboarding/welcomed").header("Authorization", auth)).andExpect(status().isNoContent());
        mvc.perform(get(base).header("Authorization", auth)).andExpect(jsonPath("$.welcomePending").value(false));
    }

    @Test
    void shouldForbidAnotherStudentFromReadingOrWritingTheSections() throws Exception {
        Student owner = register("sec", "owner");
        Student intruder = register("sec", "intruder");
        String base = owner.applicantUrl();
        String auth = bearer(intruder.token());

        mvc.perform(get(base + "/interests").header("Authorization", auth)).andExpect(status().isForbidden());
        mvc.perform(get(base + "/residence").header("Authorization", auth)).andExpect(status().isForbidden());
        mvc.perform(get(base + "/work").header("Authorization", auth)).andExpect(status().isForbidden());
        mvc.perform(put(base + "/interests").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"desiredLevel\":\"MASTER\",\"fields\":[\"LAW\"],\"cities\":[\"Beijing\"]}"))
                .andExpect(status().isForbidden());
        mvc.perform(post(base + "/onboarding/welcomed").header("Authorization", auth)).andExpect(status().isForbidden());
    }

    // ---- helpers ----

    private ResultActions education(Student s, String institution, String level, String start, String end, boolean current)
            throws Exception {
        String endJson = end == null ? "" : ",\"endDate\":\"" + end + "\"";
        return mvc.perform(post(s.applicantUrl() + "/education").header("Authorization", bearer(s.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"institution\":\"" + institution + "\",\"country\":\"EG\",\"level\":\"" + level
                        + "\",\"startDate\":\"" + start + "\"" + endJson + ",\"current\":" + current + "}"));
    }

    private ResultActions interests(Student s, String fields, String cities) throws Exception {
        return mvc.perform(put(s.applicantUrl() + "/interests").header("Authorization", bearer(s.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"desiredLevel\":\"MASTER\",\"fields\":" + fields + ",\"cities\":" + cities + "}"));
    }

    private ResultActions residence(Student s, String body) throws Exception {
        return mvc.perform(put(s.applicantUrl() + "/residence").header("Authorization", bearer(s.token()))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions work(Student s, String body) throws Exception {
        return mvc.perform(post(s.applicantUrl() + "/work").header("Authorization", bearer(s.token()))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
