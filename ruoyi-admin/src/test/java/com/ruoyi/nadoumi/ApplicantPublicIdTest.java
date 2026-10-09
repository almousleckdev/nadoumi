package com.ruoyi.nadoumi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Staff reach an applicant only by its UUID; counting 1, 2, 3 finds nothing. */
class ApplicantPublicIdTest extends AbstractStudentIntegrationTest {

    @Test
    void shouldGiveEveryApplicantAnUnguessableId() throws Exception {
        Student s = register("Uu", "Id");
        String publicId = pid(s.applicantId());

        assertThat(UUID.fromString(publicId).toString()).isEqualTo(publicId);
        assertThat(publicId).isNotEqualTo(pid(register("Other", "One").applicantId()));
    }

    @Test
    void shouldServeTheApplicantByUuid_andRefuseTheNumericId_onTheStaffApi() throws Exception {
        Student s = register("Staff", "Sees");
        createStaff("uuid_staff", "nadoumi_super_admin");
        String staff = bearer(staffToken("uuid_staff"));

        mvc.perform(get("/api/staff/applicants/" + pid(s.applicantId())).header("Authorization", staff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(pid(s.applicantId())));
        mvc.perform(get("/api/staff/applicants/" + pid(s.applicantId()) + "/education").header("Authorization", staff))
                .andExpect(status().isOk());

        mvc.perform(get("/api/staff/applicants/" + s.applicantId()).header("Authorization", staff))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/staff/applicants/" + s.applicantId() + "/education").header("Authorization", staff))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/staff/applicants/" + UUID.randomUUID()).header("Authorization", staff))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldStillCheckPermissions_whenTheUuidIsKnown() throws Exception {
        Student s = register("Perm", "Check");
        Student other = register("Nosy", "Student");

        mvc.perform(get("/api/staff/applicants/" + pid(s.applicantId())).header("Authorization", bearer(other.token())))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAcceptTheUuidOnTheStudentApiToo_whileKeepingAccessChecks() throws Exception {
        Student s = register("Own", "Data");
        Student intruder = register("In", "Truder");

        mvc.perform(get("/api/student/applicants/" + pid(s.applicantId())).header("Authorization", bearer(s.token())))
                .andExpect(status().isOk());
        mvc.perform(get("/api/student/applicants/" + pid(s.applicantId())).header("Authorization", bearer(intruder.token())))
                .andExpect(status().isForbidden());
    }
}
