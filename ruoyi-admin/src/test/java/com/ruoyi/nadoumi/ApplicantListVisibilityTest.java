package com.ruoyi.nadoumi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Staff see applicants, not sign-ups: a student who has not finished onboarding is not listed by default. */
class ApplicantListVisibilityTest extends AbstractStudentIntegrationTest {

    private List<Integer> listedIds(String staffToken, String query) throws Exception {
        String body = mvc.perform(get("/api/staff/applicants" + query).header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.content[*].id");
    }

    @Test
    void shouldHideASignUpUntilOnboardingIsComplete_butStillShowStaffCreatedApplicants() throws Exception {
        Student signUp = register("Not", "Yet");
        createStaff("vis_staff", "nadoumi_super_admin");
        String staff = staffToken("vis_staff");
        String created = mvc.perform(post("/api/staff/applicants").header("Authorization", bearer(staff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"givenName\":\"Staff\",\"familyName\":\"Made\",\"nationality\":\"MR\","
                                + "\"invitedEmail\":\"made@example.test\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int staffMade = ((Number) JsonPath.read(created, "$.id")).intValue();

        List<Integer> byDefault = listedIds(staff, "");
        assertThat(byDefault).contains(staffMade).doesNotContain((int) signUp.applicantId());

        assertThat(listedIds(staff, "?incomplete=true")).contains(staffMade, (int) signUp.applicantId());

        jdbc.update("update nad_applicant set onboarded_at = now() where id = ?", signUp.applicantId());
        assertThat(listedIds(staff, "")).contains(staffMade, (int) signUp.applicantId());
    }
}
