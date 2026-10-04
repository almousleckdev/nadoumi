package com.nadoumi.content.web.response;

import com.nadoumi.content.domain.Article;
import java.time.LocalDateTime;

/** Public feed card. No status, author id or audit fields. */
public record ArticleSummary(String slug, String title, String subtitle, String coverUrl, String language,
        String authorName, LocalDateTime publishedAt, int commentCount, int readMinutes) {

    private static final int WORDS_PER_MINUTE = 200;

    public static ArticleSummary of(Article a, String coverUrl) {
        return new ArticleSummary(a.getSlug(), a.getTitle(), a.getSubtitle(), coverUrl, a.getLanguage(),
                a.getAuthorName(), a.getPublishedAt(), a.getCommentCount() == null ? 0 : a.getCommentCount(),
                readMinutes(a.getBodyMd()));
    }

    static int readMinutes(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return 1;
        }
        int words = markdown.trim().split("\\s+").length;
        return Math.max(1, (int) Math.ceil(words / (double) WORDS_PER_MINUTE));
    }
}
