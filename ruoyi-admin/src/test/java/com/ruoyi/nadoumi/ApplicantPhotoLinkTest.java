package com.ruoyi.nadoumi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The applicants table shows each person's profile photo through a signed link, never a raw media URL. */
class ApplicantPhotoLinkTest extends AbstractStudentIntegrationTest {

    private String photoUrlOf(String listJson, long applicantId) {
        List<String> urls = JsonPath.read(listJson, "$.content[?(@.id == " + applicantId + ")].photoUrl");
        return urls.isEmpty() ? null : urls.get(0);
    }

    @Test
    void shouldGiveAWorkingPhotoLink_onlyToApplicantsThatHaveAPhoto() throws Exception {
        Student withPhoto = register("Pho", "To");
        Student without = register("No", "Photo");
        upload(withPhoto, "/photo", "me.png").andExpect(status().isCreated());
        createStaff("photo_staff", "nadoumi_super_admin");
        String staff = bearer(staffToken("photo_staff"));

        String list = mvc.perform(get("/api/staff/applicants?incomplete=true").header("Authorization", staff))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        String link = photoUrlOf(list, withPhoto.applicantId());
        assertThat(link).startsWith("/api/public/avatars/applicants/" + pid(withPhoto.applicantId()) + "/");
        assertThat(photoUrlOf(list, without.applicantId())).isNull();

        mvc.perform(get(link)).andExpect(status().isFound());
        mvc.perform(get("/api/public/avatars/applicants/" + pid(withPhoto.applicantId()) + "/forged-signature"))
                .andExpect(status().isNotFound());
        mvc.perform(get(link.replace(pid(withPhoto.applicantId()), pid(without.applicantId()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldNotExposeAPhotoLinkToTheStudentApi() throws Exception {
        Student s = register("Self", "View");
        upload(s, "/photo", "me.png").andExpect(status().isCreated());

        mvc.perform(get("/api/student/applicants").header("Authorization", bearer(s.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].photoUrl").doesNotExist());
    }
}
