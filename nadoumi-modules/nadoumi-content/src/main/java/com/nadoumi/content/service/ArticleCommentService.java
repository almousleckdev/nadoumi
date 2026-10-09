package com.nadoumi.content.service;

import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.content.domain.Article;
import com.nadoumi.content.domain.ArticleComment;
import com.nadoumi.content.domain.enums.ArticleStatus;
import com.nadoumi.content.domain.enums.CommentStatus;
import com.nadoumi.content.mapper.ArticleCommentMapper;
import com.nadoumi.content.mapper.ArticleMapper;
import com.nadoumi.content.web.request.CommentRequest;
import com.nadoumi.content.web.response.CommentNode;
import com.nadoumi.content.web.response.StaffCommentResponse;
import com.nadoumi.identity.access.CurrentCaller;
import com.nadoumi.identity.profile.PublicProfile;
import com.nadoumi.identity.profile.PublicProfileService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Threaded comments: signed-in users post and reply; staff moderate by deleting. */
@Service
public class ArticleCommentService {

    static final int MAX_COMMENTS_PER_WINDOW = 5;
    static final long WINDOW_SECONDS = 60;

    private final ArticleCommentMapper comments;
    private final ArticleMapper articles;
    private final CurrentCaller caller;
    private final PublicProfileService publicProfiles;

    public ArticleCommentService(ArticleCommentMapper comments, ArticleMapper articles, CurrentCaller caller,
            PublicProfileService publicProfiles) {
        this.comments = comments;
        this.articles = articles;
        this.caller = caller;
        this.publicProfiles = publicProfiles;
    }

    @Transactional(readOnly = true)
    public List<CommentNode> publicThread(long articleId) {
        List<ArticleComment> flat = comments.findByArticle(articleId);
        List<Long> authorIds = flat.stream()
                .filter(c -> c.getStatus() == CommentStatus.VISIBLE)
                .map(ArticleComment::getAuthorId)
                .toList();
        Map<Long, PublicProfile> profiles = publicProfiles.resolve(authorIds);
        return CommentTree.build(flat, profiles::get);
    }

    @Transactional(rollbackFor = Exception.class)
    public CommentNode post(String slug, CommentRequest req) {
        long userId = caller.requireUserId();
        Article article = articles.findBySlug(slug);
        if (article == null || article.getStatus() != ArticleStatus.PUBLISHED) {
            throw new NadNotFoundException("article not found");
        }
        if (req.parentId() != null) {
            requireReplyable(article.getId(), req.parentId());
        }
        if (comments.countByAuthorSince(userId, LocalDateTime.now().minusSeconds(WINDOW_SECONDS))
                >= MAX_COMMENTS_PER_WINDOW) {
            throw new NadBadRequestException("too many comments, please wait a moment");
        }

        ArticleComment c = new ArticleComment();
        c.setArticleId(article.getId());
        c.setParentId(req.parentId());
        c.setAuthorId(userId);
        c.setBody(req.body().trim());
        c.setStatus(CommentStatus.VISIBLE);
        comments.insert(c);

        ArticleComment saved = comments.findById(c.getId());
        PublicProfile author = publicProfiles.resolve(List.of(userId)).get(userId);
        return new CommentNode(saved.getId(), saved.getParentId(), author == null ? null : author.displayName(),
                author == null ? null : author.avatarUrl(), saved.getBody(), false, 0, saved.getCreateTime(), List.of());
    }

    /** A reply must target a visible comment of the same article; otherwise the id is treated as unknown. */
    private void requireReplyable(long articleId, long parentId) {
        ArticleComment parent = comments.findById(parentId);
        if (parent == null || parent.getArticleId() != articleId || parent.getStatus() != CommentStatus.VISIBLE) {
            throw new NadBadRequestException("cannot reply to this comment");
        }
    }

    @Transactional(readOnly = true)
    public List<StaffCommentResponse> staffList(UUID articlePublicId) {
        Article article = articles.findByPublicId(articlePublicId.toString());
        if (article == null) {
            throw new NadNotFoundException("article not found");
        }
        return comments.findByArticle(article.getId()).stream().map(StaffCommentResponse::of).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void staffDelete(long commentId, long moderatorId) {
        if (comments.findById(commentId) == null) {
            throw new NadNotFoundException("comment not found");
        }
        comments.markDeleted(commentId, moderatorId);
    }
}
