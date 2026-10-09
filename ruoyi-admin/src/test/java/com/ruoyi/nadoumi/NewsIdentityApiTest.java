package com.ruoyi.nadoumi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Who the public sees on the News pages. Staff authors show their name and photo (the photo lives in media
 * storage as a public asset). Students show a first name only: never a surname, an email address or a
 * photo. Also: the signed-in "liked by" list and related articles.
 */
class NewsIdentityApiTest extends AbstractNadIntegrationTest {

    private static final String JSON = "application/json";
    private static final byte[] PNG = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4, (byte) 0x89
    };

    @BeforeEach
    void cleanNews() {
        jdbc.update("delete from nad_article");
    }

    private record Published(String id, String slug) {
    }

    /** Creates an article as staff and publishes it (a cover id is set directly: publishing only needs one). */
    private Published publish(String staffToken, String title, String language) throws Exception {
        String created = mvc.perform(post("/api/staff/news").header("Authorization", bearer(staffToken))
                        .contentType(JSON)
                        .content("{\"title\":\"" + title + "\",\"bodyMd\":\"Body of " + title + "\",\"language\":\"" + language + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");
        jdbc.update("update nad_article set cover_media_id = 1 where public_id = ?", id);
        mvc.perform(post("/api/staff/news/{id}/publish", id).header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk());
        return new Published(id, JsonPath.read(created, "$.slug"));
    }

    private int comment(String studentToken, String slug, String body) throws Exception {
        String posted = mvc.perform(post("/api/student/news/{slug}/comments", slug)
                        .header("Authorization", bearer(studentToken)).contentType(JSON)
                        .content("{\"body\":\"" + body + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(posted, "$.id");
    }

    // ---- staff author: name and a photo the public site can load ----

    @Test
    void aStaffPhotoIsStoredAsAPublicAssetAndShownNextToTheirArticles() throws Exception {
        createStaff("news_author", "ops_manager");
        String token = staffToken("news_author");

        String uploaded = mvc.perform(multipart("/system/user/profile/avatar")
                        .file(new MockMultipartFile("avatarfile", "me.png", "image/png", PNG))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String avatarUrl = JsonPath.read(uploaded, "$.imgUrl");

        assertThat(avatarUrl).startsWith("https://");
        assertThat(jdbc.queryForObject("select avatar from sys_user where user_name = 'news_author'", String.class))
                .isEqualTo(avatarUrl);
        assertThat(jdbc.queryForObject("select access_class from nad_media_asset where category = 'STAFF_AVATAR'", String.class))
                .isEqualTo("PUBLIC");

        Published article = publish(token, "A story by staff", "en");
        mvc.perform(get("/api/public/news/{slug}", article.slug()))
                .andExpect(jsonPath("$.article.authorName").value("news_author"))
                .andExpect(jsonPath("$.article.authorAvatarUrl").value(avatarUrl));
        mvc.perform(get("/api/public/news"))
                .andExpect(jsonPath("$.content[0].authorAvatarUrl").value(avatarUrl));
        mvc.perform(get("/api/staff/news/{id}", article.id()).header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.authorAvatarUrl").value(avatarUrl));
    }

    @Test
    void aLegacyLocalPhotoPathIsNotPublished_becauseThePublicSiteCannotReachIt() throws Exception {
        createStaff("news_author2", "ops_manager");
        jdbc.update("update sys_user set avatar = '/profile/avatar/2026/01/01/old.png' where user_name = 'news_author2'");
        Published article = publish(staffToken("news_author2"), "Legacy photo", "en");

        mvc.perform(get("/api/public/news/{slug}", article.slug()))
                .andExpect(jsonPath("$.article.authorAvatarUrl").doesNotExist());
    }

    @Test
    void aStaffPhotoThatIsNotAnImageIsRejected() throws Exception {
        createStaff("news_author3", "ops_manager");

        mvc.perform(multipart("/system/user/profile/avatar")
                        .file(new MockMultipartFile("avatarfile", "evil.png", "text/html", "<script>x</script>".getBytes()))
                        .header("Authorization", bearer(staffToken("news_author3"))))
                .andExpect(status().is4xxClientError());
        assertThat(jdbc.queryForObject("select count(*) from nad_media_asset where category = 'STAFF_AVATAR'", Integer.class)).isZero();
    }

    // ---- students: first name only ----

    @Test
    void aStudentCommentShowsAFirstNameOnly_neverTheSurnameOrAPhoto() throws Exception {
        createStaff("news_author4", "ops_manager");
        Published article = publish(staffToken("news_author4"), "Privacy check", "en");
        createStudent("ava_reader");
        jdbc.update("update sys_user set nick_name = 'AVA CHEN', avatar = 'https://cdn.example/private-face.jpg' "
                + "where user_name = 'ava_reader'");

        int commentId = comment(studentToken("ava_reader"), article.slug(), "Thanks for this");

        mvc.perform(get("/api/public/news/{slug}", article.slug()))
                .andExpect(jsonPath("$.comments[0].id").value(commentId))
                .andExpect(jsonPath("$.comments[0].authorName").value("Ava"))
                .andExpect(jsonPath("$.comments[0].authorAvatarUrl").doesNotExist());
        String publicPage = mvc.perform(get("/api/public/news/{slug}", article.slug()))
                .andReturn().getResponse().getContentAsString();
        assertThat(publicPage).doesNotContain("CHEN").doesNotContain("Chen").doesNotContain("private-face")
                .doesNotContain("@example.test");
    }

    @Test
    void aStudentWithAnApplicantProfileIsShownByTheirGivenName() throws Exception {
        createStaff("news_author5", "ops_manager");
        Published article = publish(staffToken("news_author5"), "Given name", "en");
        long userId = createStudent("giulia_reader");
        jdbc.update("insert into nad_applicant (given_name, family_name, status, create_by) values ('GIULIA MARIA', 'ROSSI', 'ACTIVE', 'test')");
        Long applicantId = jdbc.queryForObject("select max(id) from nad_applicant", Long.class);
        jdbc.update("insert into nad_user_applicant_access (user_id, applicant_id, access_role, status, create_by) "
                + "values (?, ?, 'OWNER', 'ACTIVE', 'test')", userId, applicantId);

        comment(studentToken("giulia_reader"), article.slug(), "Hello");

        mvc.perform(get("/api/public/news/{slug}", article.slug()))
                .andExpect(jsonPath("$.comments[0].authorName").value("Giulia"));
    }

    @Test
    void aCommentersEmailAddressIsNeverUsedAsTheirName() throws Exception {
        createStaff("news_author6", "ops_manager");
        Published article = publish(staffToken("news_author6"), "No email", "en");
        createStudent("mail_reader");
        String token = studentToken("mail_reader"); // sign in first, then give the account an email-like username
        jdbc.update("update sys_user set nick_name = '', user_name = 'mail.reader@example.test' where user_name = 'mail_reader'");

        comment(token, article.slug(), "No names, please");

        String page = mvc.perform(get("/api/public/news/{slug}", article.slug()))
                .andExpect(jsonPath("$.comments[0].authorName").value("Student"))
                .andReturn().getResponse().getContentAsString();
        assertThat(page).doesNotContain("mail.reader").doesNotContain("example.test");
    }

    // ---- liked by ----

    @Test
    void signedInReadersSeeWhoLikedTheArticleByFirstName_newestFirst() throws Exception {
        createStaff("news_author7", "ops_manager");
        Published article = publish(staffToken("news_author7"), "Liked by", "en");
        createStudent("reader_one");
        createStudent("reader_two");
        jdbc.update("update sys_user set nick_name = 'AMINA HASSAN' where user_name = 'reader_one'");
        jdbc.update("update sys_user set nick_name = 'Luc Martin' where user_name = 'reader_two'");
        String one = bearer(studentToken("reader_one"));
        String two = bearer(studentToken("reader_two"));
        mvc.perform(put("/api/student/news/{slug}/like", article.slug()).header("Authorization", one)).andExpect(status().isOk());
        jdbc.update("update nad_article_like set create_time = create_time - interval 1 hour"); // reader_one liked earlier
        mvc.perform(put("/api/student/news/{slug}/like", article.slug()).header("Authorization", two)).andExpect(status().isOk());

        String body = mvc.perform(get("/api/student/news/{slug}/likes", article.slug()).header("Authorization", one))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.likers[0].displayName").value("Luc"))
                .andExpect(jsonPath("$.likers[1].displayName").value("Amina"))
                .andExpect(jsonPath("$.likers[1].avatarUrl").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("HASSAN").doesNotContain("Hassan").doesNotContain("Martin");
    }

    @Test
    void theLikedByListIsNotAvailableToAnonymousVisitors() throws Exception {
        createStaff("news_author8", "ops_manager");
        Published article = publish(staffToken("news_author8"), "Members only likers", "en");

        mvc.perform(get("/api/student/news/{slug}/likes", article.slug()))
                .andExpect(jsonPath("$.code").value(401));
    }

    // ---- related articles ----

    @Test
    void relatedArticlesComeFromTheSameLanguage_bestWordOverlapFirst_andNeverIncludeDraftsOrTheArticleItself() throws Exception {
        createStaff("news_author9", "ops_manager");
        String token = staffToken("news_author9");
        Published current = publish(token, "Studying in Chengdu", "en");
        Published close = publish(token, "Chengdu student life guide", "en");
        Published unrelated = publish(token, "Visa checklist for 2027", "en");
        publish(token, "Etudier a Chengdu", "fr");
        mvc.perform(post("/api/staff/news").header("Authorization", bearer(token)).contentType(JSON)
                .content("{\"title\":\"Chengdu draft not published\",\"bodyMd\":\"b\",\"language\":\"en\"}"))
                .andExpect(status().isCreated());

        String body = mvc.perform(get("/api/public/news/{slug}/related", current.slug())) // anonymous
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].slug").value(close.slug()))
                .andExpect(jsonPath("$[1].slug").value(unrelated.slug()))
                .andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<String>>read(body, "$[*].slug")).doesNotContain(current.slug());
        assertThat(body).doesNotContain("draft not published").doesNotContain("Etudier");

        mvc.perform(get("/api/public/news/{slug}/related", current.slug()).param("limit", "1"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void relatedArticlesOfAnUnknownOrUnpublishedArticleAreNotFound() throws Exception {
        createStaff("news_author10", "ops_manager");
        String created = mvc.perform(post("/api/staff/news").header("Authorization", bearer(staffToken("news_author10")))
                        .contentType(JSON).content("{\"title\":\"Still a draft\",\"bodyMd\":\"b\",\"language\":\"en\"}"))
                .andReturn().getResponse().getContentAsString();

        mvc.perform(get("/api/public/news/{slug}/related", JsonPath.<String>read(created, "$.slug")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/public/news/{slug}/related", "no-such-article")).andExpect(status().isNotFound());
    }
}
