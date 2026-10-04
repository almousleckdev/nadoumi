package com.nadoumi.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
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
import org.junit.jupiter.api.Test;

class ArticleServiceTest {

    private final ArticleMapper mapper = mock(ArticleMapper.class);
    private final MediaGateway media = mock(MediaGateway.class);
    private final ArticleCommentService comments = mock(ArticleCommentService.class);
    private final OutboxWriter outbox = mock(OutboxWriter.class);
    private final ArticleService service = new ArticleService(mapper, media, comments, outbox);

    private static Article article(long id, ArticleStatus status, Long coverMediaId) {
        Article a = new Article();
        a.setId(id);
        a.setSlug("hello-" + id);
        a.setTitle("Hello");
        a.setBodyMd("body");
        a.setStatus(status);
        a.setCoverMediaId(coverMediaId);
        return a;
    }

    @Test
    void shouldRejectPublish_whenArticleHasNoCover() {
        when(mapper.findById(1)).thenReturn(article(1, ArticleStatus.DRAFT, null));

        assertThatThrownBy(() -> service.publish(1)).isInstanceOf(NadBadRequestException.class);
        verify(mapper, never()).updateStatus(anyLong(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldPublish_whenArticleHasCover() {
        when(mapper.findById(1)).thenReturn(article(1, ArticleStatus.DRAFT, 9L));

        service.publish(1);

        verify(mapper).updateStatus(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(ArticleStatus.PUBLISHED),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldAnnounceToStudents_whenArticleIsPublishedForTheFirstTime() {
        when(mapper.findById(1)).thenReturn(article(1, ArticleStatus.DRAFT, 9L));

        service.publish(1);

        verify(outbox).write(org.mockito.ArgumentMatchers.eq("article"), org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(OutboxEventTypes.ARTICLE_PUBLISHED),
                org.mockito.ArgumentMatchers.contains("\"articleSlug\":\"hello-1\""));
    }

    @Test
    void shouldNotAnnounceAgain_whenArticleIsRepublishedAfterUnpublish() {
        Article a = article(1, ArticleStatus.UNPUBLISHED, 9L);
        a.setPublishedAt(java.time.LocalDateTime.now().minusDays(2));
        when(mapper.findById(1)).thenReturn(a);

        service.publish(1);

        verify(outbox, never()).write(org.mockito.ArgumentMatchers.anyString(), anyLong(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldRejectUnpublish_whenArticleIsNotPublished() {
        when(mapper.findById(1)).thenReturn(article(1, ArticleStatus.DRAFT, 9L));

        assertThatThrownBy(() -> service.unpublish(1)).isInstanceOf(NadBadRequestException.class);
    }

    @Test
    void shouldRejectDelete_whenArticleIsPublished() {
        when(mapper.findById(1)).thenReturn(article(1, ArticleStatus.PUBLISHED, 9L));

        assertThatThrownBy(() -> service.delete(1)).isInstanceOf(NadBadRequestException.class);
        verify(mapper, never()).delete(anyLong());
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
}
