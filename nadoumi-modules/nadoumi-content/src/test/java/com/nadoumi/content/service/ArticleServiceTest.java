package com.nadoumi.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.common.outbox.OutboxEventTypes;
import com.nadoumi.common.outbox.OutboxWriter;
import com.nadoumi.content.domain.Article;
import com.nadoumi.content.domain.enums.ArticleStatus;
import com.nadoumi.content.mapper.ArticleMapper;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ArticleServiceTest {

    private final ArticleMapper mapper = mock(ArticleMapper.class);
    private final MediaGateway media = mock(MediaGateway.class);
    private final ArticleCommentService comments = mock(ArticleCommentService.class);
    private final OutboxWriter outbox = mock(OutboxWriter.class);
    private final ArticleService service = new ArticleService(mapper, media, comments, outbox);

    private static UUID pid(long id) {
        return new UUID(0L, id);
    }

    private static Article article(long id, ArticleStatus status, Long coverMediaId) {
        Article a = new Article();
        a.setId(id);
        a.setPublicId(pid(id).toString());
        a.setSlug("hello-" + id);
        a.setTitle("Hello");
        a.setBodyMd("body");
        a.setStatus(status);
        a.setCoverMediaId(coverMediaId);
        return a;
    }

    /** Makes the article resolvable both by its public UUID (staff URLs) and by its internal id. */
    private Article given(Article a) {
        when(mapper.findByPublicId(a.getPublicId())).thenReturn(a);
        when(mapper.findById(a.getId())).thenReturn(a);
        return a;
    }

    @Test
    void shouldRejectPublish_whenArticleHasNoCover() {
        given(article(1, ArticleStatus.DRAFT, null));

        assertThatThrownBy(() -> service.publish(pid(1))).isInstanceOf(NadBadRequestException.class);
        verify(mapper, never()).updateStatus(anyLong(), any(), any());
    }

    @Test
    void shouldRejectPublish_whenArticleBodyIsBlank() {
        Article draft = given(article(1, ArticleStatus.DRAFT, 9L));
        draft.setBodyMd("  \n");

        assertThatThrownBy(() -> service.publish(pid(1))).isInstanceOf(NadBadRequestException.class)
                .hasMessageContaining("body");
        verify(mapper, never()).updateStatus(anyLong(), any(), any());
    }

    @Test
    void shouldPublish_whenArticleHasCover() {
        given(article(1, ArticleStatus.DRAFT, 9L));

        service.publish(pid(1));

        verify(mapper).updateStatus(eq(1L), eq(ArticleStatus.PUBLISHED), any());
    }

    @Test
    void shouldAnnounceToStudents_whenArticleIsPublishedForTheFirstTime() {
        given(article(1, ArticleStatus.DRAFT, 9L));

        service.publish(pid(1));

        verify(outbox).write(eq("article"), eq(1L), eq(OutboxEventTypes.ARTICLE_PUBLISHED),
                contains("\"articleSlug\":\"hello-1\""));
    }

    @Test
    void shouldNotAnnounceAgain_whenArticleIsRepublishedAfterUnpublish() {
        Article a = given(article(1, ArticleStatus.UNPUBLISHED, 9L));
        a.setPublishedAt(LocalDateTime.now().minusDays(2));

        service.publish(pid(1));

        verify(outbox, never()).write(anyString(), anyLong(), anyString(), anyString());
    }

    @Test
    void shouldRejectUnpublish_whenArticleIsNotPublished() {
        given(article(1, ArticleStatus.DRAFT, 9L));

        assertThatThrownBy(() -> service.unpublish(pid(1))).isInstanceOf(NadBadRequestException.class);
    }

    @Test
    void shouldRejectDelete_whenArticleIsPublished() {
        given(article(1, ArticleStatus.PUBLISHED, 9L));

        assertThatThrownBy(() -> service.delete(pid(1))).isInstanceOf(NadBadRequestException.class);
        verify(mapper, never()).delete(anyLong());
    }

    @Test
    void shouldDeleteByInternalId_whenDraftIsResolvedByPublicId() {
        given(article(4, ArticleStatus.DRAFT, null));

        service.delete(pid(4));

        verify(mapper).delete(4L);
    }

    @Test
    void shouldReturnNotFound_whenStaffAsksForAnUnknownPublicId() {
        when(mapper.findByPublicId(pid(404).toString())).thenReturn(null);

        assertThatThrownBy(() -> service.staffGet(pid(404))).isInstanceOf(NadNotFoundException.class);
        assertThatThrownBy(() -> service.update(pid(404), null)).isInstanceOf(NadNotFoundException.class);
        verify(mapper, never()).update(any());
    }

    @Test
    void shouldExposeThePublicUuid_andNotTheInternalSequence_whenStaffReadsAnArticle() {
        given(article(7, ArticleStatus.DRAFT, null));

        var response = service.staffGet(pid(7));

        assertThat(response.id()).isEqualTo(pid(7).toString());
    }

    @Test
    void shouldReturnNotFound_whenPublicReadsDraftOrUnpublishedArticle() {
        when(mapper.findBySlug("draft")).thenReturn(article(1, ArticleStatus.DRAFT, 9L));
        when(mapper.findBySlug("off")).thenReturn(article(2, ArticleStatus.UNPUBLISHED, 9L));
        when(mapper.findBySlug("missing")).thenReturn(null);

        assertThatThrownBy(() -> service.publicGet("draft")).isInstanceOf(NadNotFoundException.class);
        assertThatThrownBy(() -> service.publicGet("off")).isInstanceOf(NadNotFoundException.class);
        assertThatThrownBy(() -> service.publicGet("missing")).isInstanceOf(NadNotFoundException.class);
        verify(comments, never()).publicThread(anyLong());
    }

    @Test
    void shouldServePublishedArticleWithItsThread_whenPublic() {
        when(mapper.findBySlug("live")).thenReturn(article(3, ArticleStatus.PUBLISHED, 9L));

        var detail = service.publicGet("live");

        assertThat(detail.article().slug()).isEqualTo("hello-3");
        verify(comments).publicThread(3L);
    }

    // ---- related articles ----

    @Test
    void shouldOfferRelatedArticlesFromTheSameLanguage_rankedByOverlap() {
        Article current = article(3, ArticleStatus.PUBLISHED, 9L);
        current.setTitle("Studying in Chengdu");
        current.setLanguage("fr");
        Article close = article(4, ArticleStatus.PUBLISHED, 9L);
        close.setTitle("Chengdu student life");
        Article other = article(5, ArticleStatus.PUBLISHED, 9L);
        other.setTitle("Visa checklist");
        when(mapper.findBySlug("live")).thenReturn(current);
        when(mapper.findRelatedCandidates(3L, "fr", ArticleService.RELATED_POOL)).thenReturn(java.util.List.of(other, close));

        var related = service.related("live", 4);

        assertThat(related).extracting(r -> r.slug()).containsExactly("hello-4", "hello-5");
    }

    @Test
    void shouldClampTheRelatedLimit_andHideDraftsAndUnknownSlugs() {
        Article current = article(3, ArticleStatus.PUBLISHED, 9L);
        when(mapper.findBySlug("live")).thenReturn(current);
        java.util.List<Article> pool = new java.util.ArrayList<>();
        for (long id = 10; id < 40; id++) {
            pool.add(article(id, ArticleStatus.PUBLISHED, 9L));
        }
        when(mapper.findRelatedCandidates(3L, null, ArticleService.RELATED_POOL)).thenReturn(pool);

        assertThat(service.related("live", 500)).hasSize(ArticleService.MAX_RELATED);
        assertThat(service.related("live", 0)).hasSize(1);

        when(mapper.findBySlug("draft")).thenReturn(article(1, ArticleStatus.DRAFT, 9L));
        assertThatThrownBy(() -> service.related("draft", 4)).isInstanceOf(NadNotFoundException.class);
        assertThatThrownBy(() -> service.related("missing", 4)).isInstanceOf(NadNotFoundException.class);
    }
}
