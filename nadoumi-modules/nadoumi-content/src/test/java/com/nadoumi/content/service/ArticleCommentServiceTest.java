package com.nadoumi.content.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.content.domain.Article;
import com.nadoumi.content.domain.ArticleComment;
import com.nadoumi.content.domain.enums.ArticleStatus;
import com.nadoumi.content.domain.enums.CommentStatus;
import com.nadoumi.content.mapper.ArticleCommentMapper;
import com.nadoumi.content.mapper.ArticleMapper;
import com.nadoumi.content.web.request.CommentRequest;
import com.nadoumi.identity.access.CurrentCaller;
import com.nadoumi.identity.profile.PublicProfile;
import com.nadoumi.identity.profile.PublicProfileService;
import org.junit.jupiter.api.Test;

class ArticleCommentServiceTest {

    private static final long ARTICLE_ID = 10L;
    private static final long USER_ID = 77L;
    private static final long NEW_COMMENT_ID = 100L;

    private final ArticleCommentMapper comments = mock(ArticleCommentMapper.class);
    private final ArticleMapper articles = mock(ArticleMapper.class);
    private final CurrentCaller caller = mock(CurrentCaller.class);
    private final PublicProfileService profiles = mock(PublicProfileService.class);
    private final ArticleCommentService service = new ArticleCommentService(comments, articles, caller, profiles);

    private static Article article(ArticleStatus status) {
        Article a = new Article();
        a.setId(ARTICLE_ID);
        a.setStatus(status);
        return a;
    }

    private static ArticleComment comment(long id, long articleId, CommentStatus status) {
        ArticleComment c = new ArticleComment();
        c.setId(id);
        c.setArticleId(articleId);
        c.setStatus(status);
        c.setBody("hi");
        return c;
    }

    private void givenPublishedArticle() {
        when(caller.requireUserId()).thenReturn(USER_ID);
        when(articles.findBySlug("live")).thenReturn(article(ArticleStatus.PUBLISHED));
        // the real insert fills the generated key; mimic it so the service can re-read the row
        doAnswer(inv -> {
            ((ArticleComment) inv.getArgument(0)).setId(NEW_COMMENT_ID);
            return 1;
        }).when(comments).insert(any());
        when(comments.findById(NEW_COMMENT_ID)).thenReturn(comment(NEW_COMMENT_ID, ARTICLE_ID, CommentStatus.VISIBLE));
    }

    @Test
    void shouldPostTopLevelComment_whenArticleIsPublished() {
        givenPublishedArticle();

        service.post("live", new CommentRequest(null, "  Great read  "));

        verify(comments).insert(org.mockito.ArgumentMatchers.argThat(c ->
                c.getAuthorId() == USER_ID && c.getArticleId() == ARTICLE_ID
                        && "Great read".equals(c.getBody()) && c.getParentId() == null));
    }

    @Test
    void shouldRejectComment_whenArticleIsNotPublished() {
        when(caller.requireUserId()).thenReturn(USER_ID);
        when(articles.findBySlug("off")).thenReturn(article(ArticleStatus.UNPUBLISHED));

        assertThatThrownBy(() -> service.post("off", new CommentRequest(null, "x")))
                .isInstanceOf(NadNotFoundException.class);
        verify(comments, never()).insert(any());
    }

    @Test
    void shouldAllowReplyToAReply_whenParentIsVisibleInSameArticle() {
        givenPublishedArticle();
        when(comments.findById(5L)).thenReturn(comment(5, ARTICLE_ID, CommentStatus.VISIBLE));

        service.post("live", new CommentRequest(5L, "nested"));

        verify(comments).insert(org.mockito.ArgumentMatchers.argThat(c -> Long.valueOf(5L).equals(c.getParentId())));
    }

    @Test
    void shouldRejectReply_whenParentBelongsToAnotherArticle() {
        givenPublishedArticle();
        when(comments.findById(5L)).thenReturn(comment(5, 999L, CommentStatus.VISIBLE));

        assertThatThrownBy(() -> service.post("live", new CommentRequest(5L, "x")))
                .isInstanceOf(NadBadRequestException.class);
        verify(comments, never()).insert(any());
    }

    @Test
    void shouldRejectReply_whenParentWasDeleted() {
        givenPublishedArticle();
        when(comments.findById(5L)).thenReturn(comment(5, ARTICLE_ID, CommentStatus.DELETED));

        assertThatThrownBy(() -> service.post("live", new CommentRequest(5L, "x")))
                .isInstanceOf(NadBadRequestException.class);
    }

    @Test
    void shouldRejectComment_whenUserExceedsPostingLimit() {
        givenPublishedArticle();
        when(comments.countByAuthorSince(anyLong(), any())).thenReturn(ArticleCommentService.MAX_COMMENTS_PER_WINDOW);

        assertThatThrownBy(() -> service.post("live", new CommentRequest(null, "spam")))
                .isInstanceOf(NadBadRequestException.class);
        verify(comments, never()).insert(any());
    }

    @Test
    void shouldSoftDeleteComment_whenStaffModerates() {
        when(comments.findById(3L)).thenReturn(comment(3, ARTICLE_ID, CommentStatus.VISIBLE));

        service.staffDelete(3L, 1L);

        verify(comments).markDeleted(3L, 1L);
    }

    @Test
    void shouldReturnNotFound_whenStaffDeletesUnknownComment() {
        when(comments.findById(404L)).thenReturn(null);

        assertThatThrownBy(() -> service.staffDelete(404L, 1L)).isInstanceOf(NadNotFoundException.class);
    }

    // ---- who the public sees as a comment's author ----

    @Test
    void shouldReturnThePostersPublicProfile_notTheirFullName_whenTheyComment() {
        givenPublishedArticle();
        when(profiles.resolve(java.util.List.of(USER_ID)))
                .thenReturn(java.util.Map.of(USER_ID, new PublicProfile("Ava", null)));

        var node = service.post("live", new CommentRequest(null, "Great read"));

        org.assertj.core.api.Assertions.assertThat(node.authorName()).isEqualTo("Ava");
        org.assertj.core.api.Assertions.assertThat(node.authorAvatarUrl()).isNull();
    }

    @Test
    void shouldBuildTheThreadFromResolvedProfiles_andSkipDeletedAuthors() {
        ArticleComment visible = comment(1, ARTICLE_ID, CommentStatus.VISIBLE);
        visible.setAuthorId(5L);
        ArticleComment deleted = comment(2, ARTICLE_ID, CommentStatus.DELETED);
        deleted.setAuthorId(6L);
        when(comments.findByArticle(ARTICLE_ID)).thenReturn(java.util.List.of(visible, deleted));
        when(profiles.resolve(java.util.List.of(5L)))
                .thenReturn(java.util.Map.of(5L, new PublicProfile("Jane Smith", "https://cdn.example/jane.jpg")));

        var thread = service.publicThread(ARTICLE_ID);

        org.assertj.core.api.Assertions.assertThat(thread.get(0).authorName()).isEqualTo("Jane Smith");
        org.assertj.core.api.Assertions.assertThat(thread.get(0).authorAvatarUrl()).isEqualTo("https://cdn.example/jane.jpg");
        org.assertj.core.api.Assertions.assertThat(thread.get(1).authorName()).isNull();
        verify(profiles).resolve(java.util.List.of(5L)); // a deleted comment's author is never even looked up
    }
}
