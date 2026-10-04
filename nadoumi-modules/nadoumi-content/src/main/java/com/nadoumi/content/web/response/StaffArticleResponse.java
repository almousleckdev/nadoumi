package com.nadoumi.content.web.response;

import com.nadoumi.content.domain.Article;
import com.nadoumi.content.domain.enums.ArticleStatus;
import java.time.LocalDateTime;

public record StaffArticleResponse(Long id, String slug, String title, String subtitle, String bodyMd,
        Long coverMediaId, String coverUrl, String language, ArticleStatus status, LocalDateTime publishedAt,
        String authorName, int commentCount, LocalDateTime createTime, LocalDateTime updateTime) {

    public static StaffArticleResponse of(Article a, String coverUrl) {
        return new StaffArticleResponse(a.getId(), a.getSlug(), a.getTitle(), a.getSubtitle(), a.getBodyMd(),
                a.getCoverMediaId(), coverUrl, a.getLanguage(), a.getStatus(), a.getPublishedAt(),
                a.getAuthorName(), a.getCommentCount() == null ? 0 : a.getCommentCount(),
                a.getCreateTime(), a.getUpdateTime());
    }
}
