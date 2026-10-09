package com.nadoumi.identity.profile;

import com.nadoumi.identity.access.CurrentCaller;
import java.net.URI;
import java.util.Locale;

/**
 * The one place that decides what of a person is public. Staff are public authors: their nickname and
 * photo appear. Students are shown by first name only, with no photo, and a surname or an email address
 * can never come out of here. Anything that is not clearly a staff member is treated as a student, so a new
 * or unexpected user type exposes less, never more.
 */
public final class PublicProfileRules {

    public static final int MAX_NAME_LENGTH = 40;

    private static final int MAX_AVATAR_URL_LENGTH = 1024;
    private static final String FALLBACK_STUDENT_NAME = "Student";

    private PublicProfileRules() {
    }

    public static String displayName(String userType, String nickName, String userName, String ownerGivenName) {
        if (isStaff(userType)) {
            String nick = tidy(nickName);
            return cap(nick.isEmpty() ? tidy(userName) : nick);
        }
        String first = firstToken(tidy(ownerGivenName));
        if (first.isEmpty()) {
            first = firstToken(tidy(nickName));
        }
        if (first.isEmpty()) {
            String user = tidy(userName);
            return user.isEmpty() || user.contains("@") ? FALLBACK_STUDENT_NAME : cap(user);
        }
        return cap(softenCaps(first));
    }

    /** Only a staff member's photo, and only as an absolute https URL the public site can actually load. */
    public static String avatarUrl(String userType, String avatar) {
        if (!isStaff(userType) || avatar == null) {
            return null;
        }
        String url = avatar.trim();
        if (url.isEmpty() || url.length() > MAX_AVATAR_URL_LENGTH) {
            return null;
        }
        try {
            URI uri = URI.create(url);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null ? url : null;
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean isStaff(String userType) {
        return CurrentCaller.STAFF.equals(userType);
    }

    /** Drops control characters and collapses whitespace. */
    private static String tidy(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length());
        value.codePoints().filter(cp -> !Character.isISOControl(cp)).forEach(out::appendCodePoint);
        return out.toString().trim().replaceAll("\\s+", " ");
    }

    private static String firstToken(String value) {
        int space = value.indexOf(' ');
        return space < 0 ? value : value.substring(0, space);
    }

    /** Passports are printed in capitals ("AVA"); shown that way a name reads as shouting. */
    private static String softenCaps(String token) {
        boolean shouting = token.length() > 1
                && token.equals(token.toUpperCase(Locale.ROOT)) && !token.equals(token.toLowerCase(Locale.ROOT));
        if (!shouting) {
            return token;
        }
        int firstLength = Character.charCount(token.codePointAt(0));
        return token.substring(0, firstLength) + token.substring(firstLength).toLowerCase(Locale.ROOT);
    }

    private static String cap(String value) {
        if (value.codePointCount(0, value.length()) <= MAX_NAME_LENGTH) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, MAX_NAME_LENGTH));
    }
}
