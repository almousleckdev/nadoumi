package com.nadoumi.common.rules;

import com.nadoumi.common.exception.NadBadRequestException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Passport names are stored trimmed and UPPERCASE so they compare exactly with the
 * passport and never depend on how the student typed them.
 */
public final class NameRules {

    private static final Pattern VALID = Pattern.compile("\\p{L}[\\p{L} '\\-]*");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private NameRules() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new NadBadRequestException("name is required");
        }
        String name = WHITESPACE.matcher(raw.strip()).replaceAll(" ")
                .replace('’', '\'')
                .toUpperCase(Locale.ROOT);
        if (!VALID.matcher(name).matches()) {
            throw new NadBadRequestException("name may contain only letters, spaces, hyphens and apostrophes");
        }
        return name;
    }
}
