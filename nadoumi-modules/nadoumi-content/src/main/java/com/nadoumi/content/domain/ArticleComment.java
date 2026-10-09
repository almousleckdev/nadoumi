package com.nadoumi.content.domain;

import com.nadoumi.content.domain.enums.CommentStatus;
import java.time.LocalDateTime;

public class ArticleComment {
    private Long id;
    private Long articleId;
    private Long parentId;
    private Long authorId;
    /** Joined from {@code sys_user}; not a column of {@code nad_article_comment}. */
    private String authorName;
    private String body;
    private CommentStatus status;
    private Long deletedBy;
    private LocalDateTime deletedTime;
    private LocalDateTime createTime;
    /** Joined count of reader likes; not a column of {@code nad_article_comment}. */
    private Integer likeCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getArticleId() { return articleId; }
    public void setArticleId(Long articleId) { this.articleId = articleId; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public CommentStatus getStatus() { return status; }
    public void setStatus(CommentStatus status) { this.status = status; }
    public Long getDeletedBy() { return deletedBy; }
    public void setDeletedBy(Long deletedBy) { this.deletedBy = deletedBy; }
    public LocalDateTime getDeletedTime() { return deletedTime; }
    public void setDeletedTime(LocalDateTime deletedTime) { this.deletedTime = deletedTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }
}
