package com.nadoumi.identity.service;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The public handle a student chooses when registering ({@code sys_user.user_name}). It is shown in chat and on News
 * when the student has no first name yet, so it is short, lower case, and never looks like an email address.
 */
public final class StudentUsernames {

    public static final int MIN_LENGTH = 3;
    public static final int MAX_LENGTH = 20;
    private static final Pattern VALID = Pattern.compile("^[a-z0-9][a-z0-9_.]{" + (MIN_LENGTH - 1) + "," + (MAX_LENGTH - 1) + "}$");
    /** Names that would let a student pass for the platform or its staff. */
    private static final Set<String> RESERVED = Set.of("admin", "administrator", "root", "nadoumi", "support", "staff", "system");

    private StudentUsernames() {
    }

    public static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    /** True when the (normalised) handle has the right shape and is not reserved. */
    public static boolean isValid(String normalized) {
        return VALID.matcher(normalized).matches() && !RESERVED.contains(normalized);
    }
}
