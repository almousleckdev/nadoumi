package com.ruoyi.common.utils;

import java.util.Optional;

/**
 * Nadoumi password rules (spec Revision 2, D-R2-3): 8-32 characters, at least one
 * uppercase letter, one lowercase letter, one digit and one special character; and
 * the new password must not equal the current one.
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

    private PasswordPolicy()
    {
    }

    /**
     * @param raw            the candidate password
     * @param currentEncoded the caller's current BCrypt-encoded password, or {@code null}
     *                       when there is nothing to compare against (registration)
     * @return the violated rule key, or {@link Optional#empty()} when the password is acceptable
     */
    public static Optional<String> violation(String raw, String currentEncoded)
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
        if (currentEncoded != null && SecurityUtils.matchesPassword(raw, currentEncoded))
        {
            return Optional.of("password.sameAsCurrent");
        }
        return Optional.empty();
    }
}
