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
import com.nadoumi.content.domain.Article;
import com.nadoumi.content.domain.ArticleComment;
import com.nadoumi.content.domain.enums.ArticleStatus;
import com.nadoumi.content.domain.enums.CommentStatus;
import com.nadoumi.content.mapper.ArticleCommentMapper;
import com.nadoumi.content.mapper.ArticleMapper;
import com.nadoumi.content.mapper.ArticleReactionMapper;
import com.nadoumi.identity.access.CurrentCaller;
import com.nadoumi.identity.profile.PublicProfile;
import com.nadoumi.identity.profile.PublicProfileService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ArticleEngagementServiceTest {

    private static final long ARTICLE_ID = 10L;
    private static final long OTHER_ARTICLE_ID = 11L;
    private static final long USER_ID = 77L;
    private static final long COMMENT_ID = 100L;

    private final ArticleReactionMapper reactions = mock(ArticleReactionMapper.class);
    private final ArticleMapper articles = mock(ArticleMapper.class);
    private final ArticleCommentMapper comments = mock(ArticleCommentMapper.class);
    private final CurrentCaller caller = mock(CurrentCaller.class);
    private final PublicProfileService profiles = mock(PublicProfileService.class);
    private final ArticleEngagementService service =
            new ArticleEngagementService(reactions, articles, comments, caller, profiles);

    private static Article article(ArticleStatus status) {
        Article a = new Article();
        a.setId(ARTICLE_ID);
        a.setStatus(status);
        return a;
    }

    private static ArticleComment comment(long articleId, CommentStatus status) {
        ArticleComment c = new ArticleComment();
        c.setId(COMMENT_ID);
        c.setArticleId(articleId);
        c.setStatus(status);
        return c;
    }

    private void givenSignedInReaderAndPublishedArticle() {
        when(caller.requireUserId()).thenReturn(USER_ID);
        when(articles.findBySlug("live")).thenReturn(article(ArticleStatus.PUBLISHED));
    }

    // ---- article likes ----

    @Test
    void shouldRecordTheLikeAndReturnTheNewCount_whenReaderLikesAPublishedArticle() {
        givenSignedInReaderAndPublishedArticle();
        when(reactions.countArticleLikes(ARTICLE_ID)).thenReturn(5);

        var state = service.likeArticle("live");

        verify(reactions).insertArticleLike(ARTICLE_ID, USER_ID);
        assertThat(state.liked()).isTrue();
        assertThat(state.likeCount()).isEqualTo(5);
    }

    @Test
    void shouldRemoveTheLikeAndReturnTheNewCount_whenReaderUnlikes() {
        givenSignedInReaderAndPublishedArticle();
        when(reactions.countArticleLikes(ARTICLE_ID)).thenReturn(4);

        var state = service.unlikeArticle("live");

        verify(reactions).deleteArticleLike(ARTICLE_ID, USER_ID);
        assertThat(state.liked()).isFalse();
        assertThat(state.likeCount()).isEqualTo(4);
    }

    @Test
    void shouldNotLike_whenArticleIsADraftOrUnknown() {
        when(caller.requireUserId()).thenReturn(USER_ID);
        when(articles.findBySlug("draft")).thenReturn(article(ArticleStatus.DRAFT));
        when(articles.findBySlug("missing")).thenReturn(null);

        assertThatThrownBy(() -> service.likeArticle("draft")).isInstanceOf(NadNotFoundException.class);
        assertThatThrownBy(() -> service.likeArticle("missing")).isInstanceOf(NadNotFoundException.class);
        verify(reactions, never()).insertArticleLike(anyLong(), anyLong());
    }

    @Test
    void shouldRequireASignedInReader_beforeAnythingIsRead() {
        when(caller.requireUserId()).thenThrow(new IllegalStateException("no authenticated caller"));

        assertThatThrownBy(() -> service.likeArticle("live")).isInstanceOf(IllegalStateException.class);
        verify(articles, never()).findBySlug("live");
        verify(reactions, never()).insertArticleLike(anyLong(), anyLong());
    }

    // ---- comment likes ----

    @Test
    void shouldLikeAComment_whenItIsVisibleAndBelongsToTheArticle() {
        givenSignedInReaderAndPublishedArticle();
        when(comments.findById(COMMENT_ID)).thenReturn(comment(ARTICLE_ID, CommentStatus.VISIBLE));
        when(reactions.countCommentLikes(COMMENT_ID)).thenReturn(2);

        var state = service.likeComment("live", COMMENT_ID);

        verify(reactions).insertCommentLike(COMMENT_ID, USER_ID);
        assertThat(state.liked()).isTrue();
        assertThat(state.likeCount()).isEqualTo(2);
    }

    @Test
    void shouldRefuseToLikeAComment_whenItBelongsToAnotherArticle() {
        givenSignedInReaderAndPublishedArticle();
        when(comments.findById(COMMENT_ID)).thenReturn(comment(OTHER_ARTICLE_ID, CommentStatus.VISIBLE));

        assertThatThrownBy(() -> service.likeComment("live", COMMENT_ID)).isInstanceOf(NadBadRequestException.class);
        verify(reactions, never()).insertCommentLike(anyLong(), anyLong());
    }

    @Test
    void shouldRefuseToLikeAComment_whenItWasDeletedOrDoesNotExist() {
        givenSignedInReaderAndPublishedArticle();
        when(comments.findById(COMMENT_ID)).thenReturn(comment(ARTICLE_ID, CommentStatus.DELETED));
        when(comments.findById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.likeComment("live", COMMENT_ID)).isInstanceOf(NadBadRequestException.class);
        assertThatThrownBy(() -> service.likeComment("live", 999L)).isInstanceOf(NadBadRequestException.class);
        verify(reactions, never()).insertCommentLike(anyLong(), anyLong());
    }

    @Test
    void shouldUnlikeAComment_whenReaderTakesTheLikeBack() {
        givenSignedInReaderAndPublishedArticle();
        when(comments.findById(COMMENT_ID)).thenReturn(comment(ARTICLE_ID, CommentStatus.VISIBLE));
        when(reactions.countCommentLikes(COMMENT_ID)).thenReturn(0);

        var state = service.unlikeComment("live", COMMENT_ID);

        verify(reactions).deleteCommentLike(COMMENT_ID, USER_ID);
        assertThat(state.liked()).isFalse();
        assertThat(state.likeCount()).isZero();
    }

    // ---- what the signed-in reader already liked ----

    @Test
    void shouldReportWhatTheReaderLiked_forThePublishedArticle() {
        givenSignedInReaderAndPublishedArticle();
        when(reactions.articleLikedBy(ARTICLE_ID, USER_ID)).thenReturn(true);
        when(reactions.commentIdsLikedBy(ARTICLE_ID, USER_ID)).thenReturn(List.of(3L, 9L));

        var mine = service.mine("live");

        assertThat(mine.articleLiked()).isTrue();
        assertThat(mine.likedCommentIds()).containsExactly(3L, 9L);
    }

    @Test
    void shouldHideWhetherADraftExists_whenReaderAsksForTheirReactions() {
        when(caller.requireUserId()).thenReturn(USER_ID);
        when(articles.findBySlug("draft")).thenReturn(article(ArticleStatus.DRAFT));

        assertThatThrownBy(() -> service.mine("draft")).isInstanceOf(NadNotFoundException.class);
    }

    // ---- who liked ----

    @Test
    void shouldListTheMostRecentLikersWithTheirPublicProfiles_andTheTotal() {
        givenSignedInReaderAndPublishedArticle();
        when(reactions.recentArticleLikerIds(ARTICLE_ID, 20)).thenReturn(List.of(8L, 7L, 6L));
        when(reactions.countArticleLikes(ARTICLE_ID)).thenReturn(31);
        when(profiles.resolve(List.of(8L, 7L, 6L))).thenReturn(Map.of(
                8L, new PublicProfile("Amina", null), 6L, new PublicProfile("Jane Smith", "https://cdn.example/j.jpg")));

        var page = service.likers("live", 20);

        assertThat(page.total()).isEqualTo(31);
        // newest first; the user that no longer exists (7) is skipped
        assertThat(page.likers()).extracting(l -> l.displayName()).containsExactly("Amina", "Jane Smith");
        assertThat(page.likers().get(1).avatarUrl()).isEqualTo("https://cdn.example/j.jpg");
    }

    @Test
    void shouldClampTheLikersLimit() {
        givenSignedInReaderAndPublishedArticle();
        when(reactions.recentArticleLikerIds(ARTICLE_ID, ArticleEngagementService.MAX_LIKERS)).thenReturn(List.of());
        when(reactions.recentArticleLikerIds(ARTICLE_ID, 1)).thenReturn(List.of());

        service.likers("live", 5_000);
        service.likers("live", -3);

        verify(reactions).recentArticleLikerIds(ARTICLE_ID, ArticleEngagementService.MAX_LIKERS);
        verify(reactions).recentArticleLikerIds(ARTICLE_ID, 1);
    }

    @Test
    void shouldNotRevealTheLikers_toAnonymousVisitorsOrOfADraft() {
        when(caller.requireUserId()).thenThrow(new IllegalStateException("no authenticated caller"));
        assertThatThrownBy(() -> service.likers("live", 20)).isInstanceOf(IllegalStateException.class);

        org.mockito.Mockito.doReturn(USER_ID).when(caller).requireUserId();
        when(articles.findBySlug("draft")).thenReturn(article(ArticleStatus.DRAFT));
        assertThatThrownBy(() -> service.likers("draft", 20)).isInstanceOf(NadNotFoundException.class);
        verify(profiles, never()).resolve(org.mockito.ArgumentMatchers.any());
    }
}
