package com.nadoumi.content.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.nadoumi.content.domain.Article;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class RelatedArticlesTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 9, 12, 0);

    private static Article article(long id, String title, String subtitle, int daysOld) {
        Article a = new Article();
        a.setId(id);
        a.setTitle(title);
        a.setSubtitle(subtitle);
        a.setPublishedAt(NOW.minusDays(daysOld));
        return a;
    }

    private static List<Long> ids(List<Article> articles) {
        return articles.stream().map(Article::getId).toList();
    }

    @Test
    void shouldRankTheArticleSharingTheMostWordsFirst() {
        Article current = article(1, "Studying in Chengdu: a first semester guide", "Sichuan University life", 0);
        Article close = article(2, "Chengdu student life guide", "Food and study in Sichuan", 9);
        Article loose = article(3, "Visa checklist for 2027", null, 1);
        Article none = article(4, "Scholarship deadlines", null, 2);

        List<Article> ranked = RelatedArticles.rank(current, List.of(loose, none, close), 3);

        assertThat(ids(ranked).get(0)).isEqualTo(2L);
    }

    @Test
    void shouldFillWithTheNewestArticles_whenNothingSharesAWord() {
        Article current = article(1, "Alpha topic", null, 0);
        List<Article> candidates = List.of(
                article(2, "Visa checklist", null, 5),
                article(3, "Scholarship deadlines", null, 1),
                article(4, "Housing guide", null, 3));

        assertThat(ids(RelatedArticles.rank(current, candidates, 3))).containsExactly(3L, 4L, 2L);
    }

    @Test
    void shouldBreakTiesByRecency() {
        Article current = article(1, "Chengdu guide", null, 0);
        Article older = article(2, "Chengdu food", null, 10);
        Article newer = article(3, "Chengdu weather", null, 2);

        assertThat(ids(RelatedArticles.rank(current, List.of(older, newer), 2))).containsExactly(3L, 2L);
    }

    @Test
    void shouldIgnoreCommonFillerWords_soTheyCreateNoFalseMatches() {
        Article current = article(1, "The best guide to the city", null, 0);
        Article filler = article(2, "The history of the university", null, 1);
        Article real = article(3, "City guide: transport", null, 20);

        assertThat(ids(RelatedArticles.rank(current, List.of(filler, real), 1))).containsExactly(3L);
    }

    @Test
    void shouldNeverReturnTheCurrentArticle_andRespectTheLimit() {
        Article current = article(1, "Chengdu guide", null, 0);
        List<Article> candidates = List.of(current, article(2, "Chengdu a", null, 1),
                article(3, "Chengdu b", null, 2), article(4, "Chengdu c", null, 3));

        List<Article> ranked = RelatedArticles.rank(current, candidates, 2);

        assertThat(ranked).hasSize(2);
        assertThat(ids(ranked)).doesNotContain(1L);
    }

    @Test
    void shouldMatchChineseTitlesByCharacterPairs() {
        Article current = article(1, "成都大学留学指南", null, 0);
        Article related = article(2, "成都生活与留学经验", null, 30);
        Article other = article(3, "签证材料清单", null, 1);

        assertThat(ids(RelatedArticles.rank(current, List.of(other, related), 1))).containsExactly(2L);
    }

    @Test
    void shouldReturnNothing_whenThereAreNoCandidates_orTheLimitIsZero() {
        Article current = article(1, "Chengdu guide", null, 0);

        assertThat(RelatedArticles.rank(current, List.of(), 4)).isEmpty();
        assertThat(RelatedArticles.rank(current, List.of(article(2, "x", null, 1)), 0)).isEmpty();
    }

    @Test
    void shouldToleratePublishedAtBeingMissing() {
        Article current = article(1, "Chengdu guide", null, 0);
        Article undated = article(2, "Chengdu food", null, 0);
        undated.setPublishedAt(null);

        assertThat(RelatedArticles.rank(current, List.of(undated), 1)).hasSize(1);
    }
}
