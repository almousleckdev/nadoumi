package com.nadoumi.content.mapper;

import com.nadoumi.content.domain.enums.ArticleStatus;

/** Filter for {@link ArticleMapper#search}. {@code publicOnly} pins the query to PUBLISHED rows. */
public record ArticleSearch(String q, String language, ArticleStatus status, boolean publicOnly) {

    public static ArticleSearch publicFeed(String q, String language) {
        return new ArticleSearch(q, language, ArticleStatus.PUBLISHED, true);
    }

    public static ArticleSearch staff(String q, String language, ArticleStatus status) {
        return new ArticleSearch(q, language, status, false);
    }
}
