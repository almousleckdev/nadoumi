package com.nadoumi.common.text;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Function;

/**
 * URL slug helpers. A slug is lower-case ASCII kebab-case, ≤ 150 chars. Used for
 * the public-facing identifiers of universities, programmes and scholarships so
 * URLs never expose sequential database ids.
 */
public final class Slugs {

    private Slugs() {
    }

    /** Derive a slug from free text. Throws {@link IllegalArgumentException} if nothing usable remains. */
    public static String slugify(String value) {
        String s = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (s.isEmpty()) {
            throw new IllegalArgumentException("could not derive a slug from: " + value);
        }
        return s.length() > 150 ? s.substring(0, 150) : s;
    }

    /** Upper bound on suffix attempts before {@link #unique} gives up. */
    private static final int MAX_SUFFIX = 1_000;

    /**
     * De-duplicate a base slug. {@code ownerId} returns the id currently holding a
     * candidate ({@code null} or a non-positive value means free); {@code selfId}
     * is the row being saved (so it may keep its own slug). Appends {@code -2},
     * {@code -3}, … until free.
     */
    public static String unique(String base, Long selfId, Function<String, Long> ownerId) {
        String candidate = base;
        for (int n = 2; n < MAX_SUFFIX + 2; n++) {
            Long owner = ownerId.apply(candidate);
            if (owner == null || owner <= 0 || owner.equals(selfId)) {
                return candidate;
            }
            candidate = base + "-" + n;
        }
        throw new IllegalStateException("could not find a free slug for: " + base);
    }
}
