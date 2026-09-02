package com.ruoyi.common.utils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Nadoumi password rules (spec Revision 2, D-R2-3): 8-32 characters, at least one
 * uppercase letter, one lowercase letter, one digit and one special character; the
 * new password must not equal the current one; and it must not contain the user's
 * personal terms (name, email local-part).
 *
 * <p>Mirrored in {@code nadoumi-web/app/utils/passwordPolicy.ts} with identical
 * rules and message keys. The keys returned here are stable identifiers, not
 * user-facing text.
 */
public final class PasswordPolicy
{
    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 32;

    /** The special characters that satisfy the "one special character" rule. */
    public static final String SPECIALS = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";

    /** Personal terms shorter than this are ignored when checking "must not contain". */
    private static final int MIN_TERM_LENGTH = 3;

    private PasswordPolicy()
    {
    }

    public static Optional<String> violation(String raw, String currentEncoded)
    {
        return violation(raw, currentEncoded, List.of());
    }

    /**
     * @param raw            the candidate password
     * @param currentEncoded the caller's current BCrypt-encoded password, or {@code null}
     *                       when there is nothing to compare against (registration)
     * @param mustNotContain personal terms (first name, last name, email local-part…) that
     *                       must not appear in the password (case-insensitive, terms shorter
     *                       than 3 characters ignored)
     * @return the violated rule key, or {@link Optional#empty()} when the password is acceptable
     */
    public static Optional<String> violation(String raw, String currentEncoded, Collection<String> mustNotContain)
    {
        if (raw == null || raw.length() < MIN_LENGTH)
        {
            return Optional.of("password.tooShort");
        }
        if (raw.length() > MAX_LENGTH)
        {
            return Optional.of("password.tooLong");
        }
        if (raw.chars().noneMatch(Character::isUpperCase))
        {
            return Optional.of("password.needUpper");
        }
        if (raw.chars().noneMatch(Character::isLowerCase))
        {
            return Optional.of("password.needLower");
        }
        if (raw.chars().noneMatch(Character::isDigit))
        {
            return Optional.of("password.needDigit");
        }
        if (raw.chars().noneMatch(c -> SPECIALS.indexOf(c) >= 0))
        {
            return Optional.of("password.needSpecial");
        }
        if (containsPersonalTerm(raw, mustNotContain))
        {
            return Optional.of("password.noPersonal");
        }
        if (currentEncoded != null && SecurityUtils.matchesPassword(raw, currentEncoded))
        {
            return Optional.of("password.sameAsCurrent");
        }
        return Optional.empty();
    }

    private static boolean containsPersonalTerm(String raw, Collection<String> terms)
    {
        if (terms == null)
        {
            return false;
        }
        String lower = raw.toLowerCase();
        return terms.stream()
                .filter(StringUtils::isNotEmpty)
                .map(t -> t.trim().toLowerCase())
                .filter(t -> t.length() >= MIN_TERM_LENGTH)
                .anyMatch(lower::contains);
    }
}
