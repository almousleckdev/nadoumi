package com.nadoumi.content.domain.enums;

/** {@code nad_article_comment.status}. DELETED rows stay so replies keep their place in the thread. */
public enum CommentStatus {
    VISIBLE,
    DELETED
}
