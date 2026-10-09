package com.nadoumi.content.service;

import com.nadoumi.content.domain.Article;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * "More to read": ranks candidate articles against the one being read by how many significant words
 * their title and subtitle share, newest first among equals. Articles carry no tags or categories, so
 * words are the only honest signal; when nothing overlaps the newest articles fill the list. Latin
 * words need four letters and are not filler; Chinese is compared as pairs of adjacent characters.
 */
final class RelatedArticles {

    private static final int MIN_WORD_LENGTH = 4;
    private static final Set<String> FILLER = Set.of(
            "about", "after", "also", "been", "best", "from", "have", "into", "more", "most", "over", "some",
            "than", "that", "their", "them", "then", "there", "these", "they", "this", "those", "very", "what",
            "when", "where", "which", "while", "with", "will", "your", "ours", "should", "could", "would");

    private RelatedArticles() {
    }

    static List<Article> rank(Article current, List<Article> candidates, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        Set<String> reference = words(current);
        Comparator<Scored> order = Comparator.comparingInt(Scored::score).reversed()
                .thenComparing(s -> s.article().getPublishedAt(), Comparator.nullsFirst(Comparator.<LocalDateTime>naturalOrder()).reversed())
                .thenComparing(s -> s.article().getId(), Comparator.reverseOrder());
        return candidates.stream()
                .filter(a -> !a.getId().equals(current.getId()))
                .map(a -> new Scored(a, overlap(reference, words(a))))
                .sorted(order)
                .limit(limit)
                .map(Scored::article)
                .toList();
    }

    private static int overlap(Set<String> a, Set<String> b) {
        int shared = 0;
        for (String word : a) {
            if (b.contains(word)) {
                shared++;
            }
        }
        return shared;
    }

    private static Set<String> words(Article a) {
        String text = (a.getTitle() == null ? "" : a.getTitle()) + " " + (a.getSubtitle() == null ? "" : a.getSubtitle());
        Set<String> words = new HashSet<>();
        List<Integer> han = new ArrayList<>();
        StringBuilder latin = new StringBuilder();
        for (int cp : text.toLowerCase(Locale.ROOT).codePoints().toArray()) {
            boolean isHan = Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN;
            if (isHan) {
                flushLatin(latin, words);
                han.add(cp);
                continue;
            }
            flushHan(han, words);
            if (Character.isLetter(cp)) {
                latin.appendCodePoint(cp);
            }
            else {
                flushLatin(latin, words);
            }
        }
        flushLatin(latin, words);
        flushHan(han, words);
        return words;
    }

    private static void flushLatin(StringBuilder latin, Set<String> words) {
        if (latin.length() >= MIN_WORD_LENGTH && !FILLER.contains(latin.toString())) {
            words.add(latin.toString());
        }
        latin.setLength(0);
    }

    private static void flushHan(List<Integer> han, Set<String> words) {
        for (int i = 0; i + 1 < han.size(); i++) {
            words.add(new String(Character.toChars(han.get(i))) + new String(Character.toChars(han.get(i + 1))));
        }
        han.clear();
    }

    private record Scored(Article article, int score) {
    }
}
