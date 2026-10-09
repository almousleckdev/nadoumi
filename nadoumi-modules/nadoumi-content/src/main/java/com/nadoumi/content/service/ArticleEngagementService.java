package com.nadoumi.content.service;

import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.content.domain.Article;
import com.nadoumi.content.domain.ArticleComment;
import com.nadoumi.content.domain.enums.ArticleStatus;
import com.nadoumi.content.domain.enums.CommentStatus;
import com.nadoumi.content.mapper.ArticleCommentMapper;
import com.nadoumi.content.mapper.ArticleMapper;
import com.nadoumi.content.mapper.ArticleReactionMapper;
import com.nadoumi.content.web.response.LikeState;
import com.nadoumi.content.web.response.Liker;
import com.nadoumi.content.web.response.LikersPage;
import com.nadoumi.content.web.response.MyReactions;
import com.nadoumi.identity.access.CurrentCaller;
import com.nadoumi.identity.profile.PublicProfile;
import com.nadoumi.identity.profile.PublicProfileService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reader likes on published articles and their comments. Every operation is idempotent (liking twice
 * is one like) so a retried request or a double click can never inflate a count.
 */
@Service
public class ArticleEngagementService {

    static final int DEFAULT_LIKERS = 20;
    static final int MAX_LIKERS = 50;

    private final ArticleReactionMapper reactions;
    private final ArticleMapper articles;
    private final ArticleCommentMapper comments;
    private final CurrentCaller caller;
    private final PublicProfileService publicProfiles;

    public ArticleEngagementService(ArticleReactionMapper reactions, ArticleMapper articles,
            ArticleCommentMapper comments, CurrentCaller caller, PublicProfileService publicProfiles) {
        this.reactions = reactions;
        this.articles = articles;
        this.comments = comments;
        this.caller = caller;
        this.publicProfiles = publicProfiles;
    }

    @Transactional(rollbackFor = Exception.class)
    public LikeState likeArticle(String slug) {
        long userId = caller.requireUserId();
        long articleId = publishedArticle(slug).getId();
        reactions.insertArticleLike(articleId, userId);
        return new LikeState(true, reactions.countArticleLikes(articleId));
    }

    @Transactional(rollbackFor = Exception.class)
    public LikeState unlikeArticle(String slug) {
        long userId = caller.requireUserId();
        long articleId = publishedArticle(slug).getId();
        reactions.deleteArticleLike(articleId, userId);
        return new LikeState(false, reactions.countArticleLikes(articleId));
    }

    @Transactional(rollbackFor = Exception.class)
    public LikeState likeComment(String slug, long commentId) {
        long userId = caller.requireUserId();
        requireLikeableComment(publishedArticle(slug).getId(), commentId);
        reactions.insertCommentLike(commentId, userId);
        return new LikeState(true, reactions.countCommentLikes(commentId));
    }

    @Transactional(rollbackFor = Exception.class)
    public LikeState unlikeComment(String slug, long commentId) {
        long userId = caller.requireUserId();
        requireLikeableComment(publishedArticle(slug).getId(), commentId);
        reactions.deleteCommentLike(commentId, userId);
        return new LikeState(false, reactions.countCommentLikes(commentId));
    }

    @Transactional(readOnly = true)
    public MyReactions mine(String slug) {
        long userId = caller.requireUserId();
        long articleId = publishedArticle(slug).getId();
        return new MyReactions(reactions.articleLikedBy(articleId, userId),
                reactions.commentIdsLikedBy(articleId, userId));
    }

    /** Who liked the article, most recent first. Readers appear the way {@code PublicProfileRules} allows. */
    @Transactional(readOnly = true)
    public LikersPage likers(String slug, int limit) {
        caller.requireUserId();
        long articleId = publishedArticle(slug).getId();
        int size = Math.min(Math.max(limit, 1), MAX_LIKERS);
        List<Long> ids = reactions.recentArticleLikerIds(articleId, size);
        Map<Long, PublicProfile> profiles = publicProfiles.resolve(ids);
        List<Liker> likers = ids.stream()
                .map(profiles::get)
                .filter(java.util.Objects::nonNull)
                .map(p -> new Liker(p.displayName(), p.avatarUrl()))
                .toList();
        return new LikersPage(reactions.countArticleLikes(articleId), likers);
    }

    /** A draft or unpublished article is indistinguishable from a missing one. */
    private Article publishedArticle(String slug) {
        Article article = articles.findBySlug(slug);
        if (article == null || article.getStatus() != ArticleStatus.PUBLISHED) {
            throw new NadNotFoundException("article not found");
        }
        return article;
    }

    /** The comment must be a visible one of this very article; anything else is treated as unknown. */
    private void requireLikeableComment(long articleId, long commentId) {
        ArticleComment comment = comments.findById(commentId);
        if (comment == null || comment.getArticleId() != articleId || comment.getStatus() != CommentStatus.VISIBLE) {
            throw new NadBadRequestException("cannot like this comment");
        }
    }
}
