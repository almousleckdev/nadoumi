package com.nadoumi.content.web.response;

import com.nadoumi.content.domain.ArticleComment;
import com.nadoumi.content.domain.enums.CommentStatus;
import java.time.LocalDateTime;

/** Moderation view: the full body is kept even after deletion, for audit. */
public record StaffCommentResponse(Long id, Long articleId, Long parentId, Long authorId, String authorName,
        String body, CommentStatus status, LocalDateTime createTime, LocalDateTime deletedTime) {

    public static StaffCommentResponse of(ArticleComment c) {
        return new StaffCommentResponse(c.getId(), c.getArticleId(), c.getParentId(), c.getAuthorId(),
                c.getAuthorName(), c.getBody(), c.getStatus(), c.getCreateTime(), c.getDeletedTime());
    }
}
