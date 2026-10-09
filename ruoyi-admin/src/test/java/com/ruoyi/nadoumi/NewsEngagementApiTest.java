package com.ruoyi.nadoumi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * News end to end over the real context: staff address articles by an opaque UUID (never the
 * guessable sequence), and signed-in readers like articles and comments, idempotently, with the
 * totals visible to anonymous visitors.
 */
class NewsEngagementApiTest extends AbstractNadIntegrationTest {

    private static final Pattern UUID_SHAPE =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    private static final String JSON = "application/json";
    private static final String ARTICLE_BODY =
            "{\"title\":\"Studying in Chengdu\",\"subtitle\":\"A first look\",\"bodyMd\":\"Hello **reader**\",\"language\":\"en\"}";

    @BeforeEach
    void cleanNews() {
        jdbc.update("delete from nad_article"); // cascades comments, likes and image rows
    }

    /** Creates and publishes an article as staff; returns its public UUID and slug. */
    private String[] publishedArticle(String staffToken) throws Exception {
        String created = mvc.perform(post("/api/staff/news").header("Authorization", bearer(staffToken))
                        .contentType(JSON).content(ARTICLE_BODY))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");
        String slug = JsonPath.read(created, "$.slug");
        jdbc.update("update nad_article set cover_media_id = 1 where public_id = ?", id); // publishing needs a cover
        mvc.perform(post("/api/staff/news/{id}/publish", id).header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
        return new String[] { id, slug };
    }

    // ---- UUID identifiers ----

    @Test
    void staffAddressArticlesByAnOpaqueUuid_notBySequenceNumber() throws Exception {
        createStaff("news_editor", "ops_manager");
        String token = bearer(staffToken("news_editor"));

        String created = mvc.perform(post("/api/staff/news").header("Authorization", token)
                        .contentType(JSON).content(ARTICLE_BODY))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");
        assertThat(id).matches(UUID_SHAPE);

        mvc.perform(get("/api/staff/news/{id}", id).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Studying in Chengdu"));

        Long internalId = jdbc.queryForObject("select id from nad_article where public_id = ?", Long.class, id);
        mvc.perform(get("/api/staff/news/{id}", internalId).header("Authorization", token))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void staffGetsNotFound_forAnUnknownUuid() throws Exception {
        createStaff("news_editor2", "ops_manager");

        mvc.perform(get("/api/staff/news/{id}", "00000000-0000-0000-0000-000000000000")
                        .header("Authorization", bearer(staffToken("news_editor2"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void staffCanSaveADraftWithAnEmptyBody_butCannotPublishIt() throws Exception {
        createStaff("news_editor3", "ops_manager");
        String token = bearer(staffToken("news_editor3"));

        String created = mvc.perform(post("/api/staff/news").header("Authorization", token).contentType(JSON)
                        .content("{\"title\":\"Just a title\",\"bodyMd\":\"\",\"language\":\"en\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");
        jdbc.update("update nad_article set cover_media_id = 1 where public_id = ?", id);

        mvc.perform(post("/api/staff/news/{id}/publish", id).header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    // ---- article likes ----

    @Test
    void readerLikesAnArticle_once_andAnonymousVisitorsSeeTheTotal() throws Exception {
        createStaff("news_editor4", "ops_manager");
        String[] article = publishedArticle(staffToken("news_editor4"));
        String slug = article[1];
        createStudent("reader_a");
        createStudent("reader_b");
        String a = bearer(studentToken("reader_a"));
        String b = bearer(studentToken("reader_b"));

        mvc.perform(put("/api/student/news/{slug}/like", slug).header("Authorization", a))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true))
                .andExpect(jsonPath("$.likeCount").value(1));
        // a repeated like (double click, retry) never inflates the count
        mvc.perform(put("/api/student/news/{slug}/like", slug).header("Authorization", a))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1));
        mvc.perform(put("/api/student/news/{slug}/like", slug).header("Authorization", b))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(2));

        mvc.perform(get("/api/public/news/{slug}", slug)) // anonymous
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.article.likeCount").value(2));
        mvc.perform(get("/api/public/news"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].likeCount").value(2));

        mvc.perform(get("/api/student/news/{slug}/reactions", slug).header("Authorization", a))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.articleLiked").value(true));

        mvc.perform(delete("/api/student/news/{slug}/like", slug).header("Authorization", a))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(false))
                .andExpect(jsonPath("$.likeCount").value(1));
        mvc.perform(get("/api/student/news/{slug}/reactions", slug).header("Authorization", a))
                .andExpect(jsonPath("$.articleLiked").value(false));
        mvc.perform(get("/api/student/news/{slug}/reactions", slug).header("Authorization", b))
                .andExpect(jsonPath("$.articleLiked").value(true));
    }

    /** RuoYi renders a rejected anonymous call as HTTP 200 with {@code {"code":401}} in the body. */
    @Test
    void anonymousVisitorCannotLike() throws Exception {
        jdbc.update("insert into nad_article (slug, title, body_md, status, cover_media_id, author_id, published_at) "
                + "values ('anon-target', 'Anon target', 'b', 'PUBLISHED', 1, 1, now())");

        mvc.perform(put("/api/student/news/{slug}/like", "anon-target"))
                .andExpect(jsonPath("$.code").value(401));
        mvc.perform(get("/api/student/news/{slug}/reactions", "anon-target"))
                .andExpect(jsonPath("$.code").value(401));
        mvc.perform(put("/api/student/news/{slug}/comments/{id}/like", "anon-target", 1))
                .andExpect(jsonPath("$.code").value(401));
        assertThat(jdbc.queryForObject("select count(*) from nad_article_like", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from nad_article_comment_like", Integer.class)).isZero();
    }

    @Test
    void aDraftCannotBeLiked_andItsExistenceIsNotRevealed() throws Exception {
        createStaff("news_editor6", "ops_manager");
        String token = bearer(staffToken("news_editor6"));
        String created = mvc.perform(post("/api/staff/news").header("Authorization", token)
                        .contentType(JSON).content(ARTICLE_BODY))
                .andReturn().getResponse().getContentAsString();
        String draftSlug = JsonPath.read(created, "$.slug");
        createStudent("reader_c");

        mvc.perform(put("/api/student/news/{slug}/like", draftSlug)
                        .header("Authorization", bearer(studentToken("reader_c"))))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/student/news/{slug}/like", "no-such-article")
                        .header("Authorization", bearer(studentToken("reader_c"))))
                .andExpect(status().isNotFound());
    }

    // ---- comment likes ----

    @Test
    void readerLikesAComment_andTheThreadCarriesTheTotals() throws Exception {
        createStaff("news_editor7", "ops_manager");
        String[] article = publishedArticle(staffToken("news_editor7"));
        String slug = article[1];
        createStudent("reader_d");
        createStudent("reader_e");
        String d = bearer(studentToken("reader_d"));
        String e = bearer(studentToken("reader_e"));

        String posted = mvc.perform(post("/api/student/news/{slug}/comments", slug).header("Authorization", d)
                        .contentType(JSON).content("{\"body\":\"Great read\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int commentId = JsonPath.read(posted, "$.id");

        mvc.perform(put("/api/student/news/{slug}/comments/{id}/like", slug, commentId).header("Authorization", d))
                .andExpect(status().isOk()).andExpect(jsonPath("$.likeCount").value(1));
        mvc.perform(put("/api/student/news/{slug}/comments/{id}/like", slug, commentId).header("Authorization", e))
                .andExpect(status().isOk()).andExpect(jsonPath("$.likeCount").value(2));
        mvc.perform(put("/api/student/news/{slug}/comments/{id}/like", slug, commentId).header("Authorization", e))
                .andExpect(status().isOk()).andExpect(jsonPath("$.likeCount").value(2));

        mvc.perform(get("/api/public/news/{slug}", slug))
                .andExpect(jsonPath("$.comments[0].likeCount").value(2));
        mvc.perform(get("/api/student/news/{slug}/reactions", slug).header("Authorization", e))
                .andExpect(jsonPath("$.likedCommentIds[0]").value(commentId));

        mvc.perform(delete("/api/student/news/{slug}/comments/{id}/like", slug, commentId).header("Authorization", e))
                .andExpect(status().isOk()).andExpect(jsonPath("$.likeCount").value(1));
    }

    @Test
    void aCommentOfAnotherArticleCannotBeLikedThroughThisOne() throws Exception {
        createStaff("news_editor8", "ops_manager");
        String staff = staffToken("news_editor8");
        String[] first = publishedArticle(staff);
        mvc.perform(post("/api/staff/news").header("Authorization", bearer(staff)).contentType(JSON)
                .content("{\"title\":\"Another story\",\"bodyMd\":\"b\",\"language\":\"en\"}")).andExpect(status().isCreated());
        String otherSlug = jdbc.queryForObject("select slug from nad_article where title = 'Another story'", String.class);
        jdbc.update("update nad_article set status = 'PUBLISHED', cover_media_id = 1 where slug = ?", otherSlug);
        createStudent("reader_f");
        String f = bearer(studentToken("reader_f"));
        String posted = mvc.perform(post("/api/student/news/{slug}/comments", otherSlug).header("Authorization", f)
                        .contentType(JSON).content("{\"body\":\"elsewhere\"}"))
                .andReturn().getResponse().getContentAsString();
        int foreignComment = JsonPath.read(posted, "$.id");

        mvc.perform(put("/api/student/news/{slug}/comments/{id}/like", first[1], foreignComment)
                        .header("Authorization", f))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select count(*) from nad_article_comment_like", Integer.class)).isZero();
    }

    @Test
    void aDeletedCommentCannotBeLiked() throws Exception {
        createStaff("news_editor9", "ops_manager");
        String[] article = publishedArticle(staffToken("news_editor9"));
        createStudent("reader_g");
        String g = bearer(studentToken("reader_g"));
        String posted = mvc.perform(post("/api/student/news/{slug}/comments", article[1]).header("Authorization", g)
                        .contentType(JSON).content("{\"body\":\"to be removed\"}"))
                .andReturn().getResponse().getContentAsString();
        int commentId = JsonPath.read(posted, "$.id");
        jdbc.update("update nad_article_comment set status = 'DELETED' where id = ?", commentId);

        mvc.perform(put("/api/student/news/{slug}/comments/{id}/like", article[1], commentId).header("Authorization", g))
                .andExpect(status().isBadRequest());
    }
}
