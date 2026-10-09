package com.ruoyi.nadoumi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.nadoumi.identity.profile.AvatarLinks;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Profile pictures in chat and on the News pages: a staff photo is a public asset, a student's photo is private
 * media reached only through a signed link the server hands out where that student already appears.
 */
class ProfilePhotoApiTest extends AbstractNadIntegrationTest {

    private static final String JSON = "application/json";
    private static final byte[] PNG = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4, (byte) 0x89
    };

    @Autowired
    private AvatarLinks links;

    /** A student account that owns an applicant profile, optionally with an uploaded photo. */
    private long studentWithApplicant(String username, boolean withPhoto) throws Exception {
        long userId = createStudent(username);
        jdbc.update("insert into nad_applicant (given_name, family_name, email, status, onboarded_at, welcomed_at, "
                + "create_by, create_time, update_by) values ('Ava', 'Chen', ?, 'ACTIVE', now(), now(), 'test', now(), 'test')",
                username + "@example.test");
        long applicantId = jdbc.queryForObject("select max(id) from nad_applicant", Long.class);
        jdbc.update("insert into nad_user_applicant_access (user_id, applicant_id, access_role, status, granted_by_user_id, "
                + "granted_at, create_by, create_time, update_by) values (?, ?, 'OWNER', 'ACTIVE', ?, now(), 'test', now(), 'test')",
                userId, applicantId, userId);
        if (withPhoto) {
            mvc.perform(multipart("/api/student/applicants/{id}/photo", applicantId)
                            .file(new MockMultipartFile("file", "me.png", "image/png", PNG))
                            .header("Authorization", bearer(studentToken(username))))
                    .andExpect(status().isCreated());
        }
        return userId;
    }

    private void studentOpensChat(String username, long staffUserId) throws Exception {
        mvc.perform(post("/api/student/conversations/direct").header("Authorization", bearer(studentToken(username)))
                        .contentType(JSON).content("{\"userId\":" + staffUserId + "}"))
                .andExpect(status().isOk());
    }

    @Test
    void aStudentPhotoShowsInTheStaffChatThroughASignedLink_thatRedirectsToTheMedia() throws Exception {
        long staff = createStaff("photo_staff", "ops_manager");
        long student = studentWithApplicant("photo_stu", true);
        studentOpensChat("photo_stu", staff);

        String inbox = mvc.perform(get("/api/staff/conversations").header("Authorization", bearer(staffToken("photo_staff"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String link = JsonPath.read(inbox, "$[0].peer.avatarUrl");

        assertThat(link).startsWith("/api/public/avatars/" + student + "/");
        mvc.perform(get(link)).andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("https://")))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("max-age=60")));
    }

    @Test
    void aLinkWithAWrongSignatureOrForAStudentWithoutAPhotoIsA404() throws Exception {
        long staff = createStaff("photo_staff2", "ops_manager");
        long withPhoto = studentWithApplicant("photo_stu2", true);
        long withoutPhoto = studentWithApplicant("photo_stu3", false);
        studentOpensChat("photo_stu3", staff);

        mvc.perform(get("/api/public/avatars/{id}/{sig}", withPhoto, "not-the-signature")).andExpect(status().isNotFound());
        mvc.perform(get(links.linkFor(withoutPhoto))).andExpect(status().isNotFound());
        mvc.perform(get("/api/staff/conversations").header("Authorization", bearer(staffToken("photo_staff2"))))
                .andExpect(jsonPath("$[0].peer.avatarUrl").doesNotExist());
    }

    @Test
    void aStaffPhotoShowsToTheStudentInChat_asItsPublicUrl() throws Exception {
        createStaff("photo_staff4", "ops_manager");
        studentWithApplicant("photo_stu4", false);
        String uploaded = mvc.perform(multipart("/system/user/profile/avatar")
                        .file(new MockMultipartFile("avatarfile", "me.png", "image/png", PNG))
                        .header("Authorization", bearer(staffToken("photo_staff4"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String url = JsonPath.read(uploaded, "$.imgUrl");
        long staffId = jdbc.queryForObject("select user_id from sys_user where user_name = 'photo_staff4'", Long.class);
        studentOpensChat("photo_stu4", staffId);

        mvc.perform(get("/api/student/conversations").header("Authorization", bearer(studentToken("photo_stu4"))))
                .andExpect(jsonPath("$[0].peer.avatarUrl").value(url));
        mvc.perform(get("/api/student/conversations/staff").header("Authorization", bearer(studentToken("photo_stu4"))))
                .andExpect(jsonPath("$[?(@.name=='photo_staff4')].avatarUrl").value(url));
    }

    @Test
    void aStudentWhoCommentsShowsTheirPhotoNextToTheCommentOnTheNewsPage() throws Exception {
        createStaff("photo_author", "ops_manager");
        studentWithApplicant("photo_commenter", true);
        String staff = staffToken("photo_author");
        String created = mvc.perform(post("/api/staff/news").header("Authorization", bearer(staff)).contentType(JSON)
                        .content("{\"title\":\"Photo story\",\"bodyMd\":\"Body\",\"language\":\"en\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");
        String slug = JsonPath.read(created, "$.slug");
        jdbc.update("update nad_article set cover_media_id = 1 where public_id = ?", id);
        mvc.perform(post("/api/staff/news/{id}/publish", id).header("Authorization", bearer(staff))).andExpect(status().isOk());
        mvc.perform(post("/api/student/news/{slug}/comments", slug).header("Authorization", bearer(studentToken("photo_commenter")))
                        .contentType(JSON).content("{\"body\":\"Great read\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/public/news/{slug}", slug))
                .andExpect(jsonPath("$.comments[0].authorName").value("Ava"))
                .andExpect(jsonPath("$.comments[0].authorAvatarUrl").value(org.hamcrest.Matchers.startsWith("/api/public/avatars/")));
    }

    @Test
    void aRealisticallyLongCloudinaryUrlIsStoredInFullAndShownToTheStudent() throws Exception {
        long staff = createStaff("photo_staff5", "ops_manager");
        studentWithApplicant("photo_stu5", false);
        String longUrl = "https://res.cloudinary.com/demo/image/upload/v1760000000/nadoumi/staff/avatar/" + "a".repeat(140) + ".jpg";
        jdbc.update("update sys_user set avatar = ? where user_id = ?", longUrl, staff);

        assertThat(longUrl.length()).isGreaterThan(100);
        studentOpensChat("photo_stu5", staff);
        mvc.perform(get("/api/student/conversations").header("Authorization", bearer(studentToken("photo_stu5"))))
                .andExpect(jsonPath("$[0].peer.avatarUrl").value(longUrl));
    }
}
