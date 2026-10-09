package com.nadoumi.content.web.response;

import com.nadoumi.content.domain.Article;
import com.nadoumi.identity.profile.PublicProfileRules;
import java.time.LocalDateTime;

/** Public feed card. No status, author id or audit fields. */
public record ArticleSummary(String slug, String title, String subtitle, String coverUrl, String language,
        String authorName, String authorAvatarUrl, LocalDateTime publishedAt, int commentCount, int likeCount,
        int readMinutes) {

    private static final int WORDS_PER_MINUTE = 200;

    public static ArticleSummary of(Article a, String coverUrl) {
        return new ArticleSummary(a.getSlug(), a.getTitle(), a.getSubtitle(), coverUrl, a.getLanguage(),
                a.getAuthorName(), PublicProfileRules.avatarUrl(a.getAuthorType(), a.getAuthorAvatar()),
                a.getPublishedAt(), a.getCommentCount() == null ? 0 : a.getCommentCount(),
                a.getLikeCount() == null ? 0 : a.getLikeCount(), readMinutes(a.getBodyMd()));
    }

    static int readMinutes(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return 1;
        }
        int words = markdown.trim().split("\\s+").length;
        return Math.max(1, (int) Math.ceil(words / (double) WORDS_PER_MINUTE));
    }
}
