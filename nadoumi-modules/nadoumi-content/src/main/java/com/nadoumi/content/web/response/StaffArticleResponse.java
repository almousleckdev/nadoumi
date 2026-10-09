package com.nadoumi.content.web.response;

import com.nadoumi.content.domain.Article;
import com.nadoumi.content.domain.enums.ArticleStatus;
import com.nadoumi.identity.profile.PublicProfileRules;
import java.time.LocalDateTime;

/** {@code id} is the opaque public UUID; the internal sequence number never leaves the service. */
public record StaffArticleResponse(String id, String slug, String title, String subtitle, String bodyMd,
        Long coverMediaId, String coverUrl, String language, ArticleStatus status, LocalDateTime publishedAt,
        String authorName, String authorAvatarUrl, int commentCount, int likeCount, LocalDateTime createTime, LocalDateTime updateTime) {

    public static StaffArticleResponse of(Article a, String coverUrl) {
        return new StaffArticleResponse(a.getPublicId(), a.getSlug(), a.getTitle(), a.getSubtitle(), a.getBodyMd(),
                a.getCoverMediaId(), coverUrl, a.getLanguage(), a.getStatus(), a.getPublishedAt(),
                a.getAuthorName(), PublicProfileRules.avatarUrl(a.getAuthorType(), a.getAuthorAvatar()),
                a.getCommentCount() == null ? 0 : a.getCommentCount(),
                a.getLikeCount() == null ? 0 : a.getLikeCount(), a.getCreateTime(), a.getUpdateTime());
    }
}
