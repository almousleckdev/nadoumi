package com.nadoumi.applicant.rules;

import com.nadoumi.common.exception.NadBadRequestException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Passport acceptance and the profile-versus-passport comparison. Pure, so the
 * server-side save and the onboarding completion check share one definition.
 */
public final class PassportRules {

    /** A passport must remain valid for more than this long to be accepted. */
    public static final int MINIMUM_VALIDITY_MONTHS = 6;

    public static final String GIVEN_NAME = "givenName";
    public static final String FAMILY_NAME = "familyName";
    public static final String DOB = "dob";

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NAME_SEPARATORS = Pattern.compile("[-\\s]+");
    private static final Pattern APOSTROPHES = Pattern.compile("['’]");

    private PassportRules() {
    }

    public static void requireAcceptable(LocalDate issueDate, LocalDate expiryDate, LocalDate today) {
        if (issueDate.isAfter(today)) {
            throw new NadBadRequestException("passport issue date cannot be in the future");
        }
        if (!expiryDate.isAfter(issueDate)) {
            throw new NadBadRequestException("passport expiry date must be after the issue date");
        }
        if (!isValidForAdmission(expiryDate, today)) {
            throw new NadBadRequestException(
                    "passport must be valid for more than six months from today");
        }
    }

    public static boolean isValidForAdmission(LocalDate expiryDate, LocalDate today) {
        return expiryDate != null && expiryDate.isAfter(today.plusMonths(MINIMUM_VALIDITY_MONTHS));
    }

    /**
     * The profile fields that differ from what the passport says. The machine-readable
     * zone drops apostrophes, turns hyphens into spaces and strips accents, so names are
     * compared after the same folding.
     */
    public static List<String> mismatches(String profileGiven, String profileFamily, LocalDate profileDob,
            String passportGiven, String passportFamily, LocalDate passportDob) {
        List<String> differing = new ArrayList<>();
        if (!sameName(profileGiven, passportGiven)) {
            differing.add(GIVEN_NAME);
        }
        if (!sameName(profileFamily, passportFamily)) {
            differing.add(FAMILY_NAME);
        }
        if (profileDob == null || !profileDob.equals(passportDob)) {
            differing.add(DOB);
        }
        return differing;
    }

    private static boolean sameName(String profile, String passport) {
        return profile != null && passport != null && fold(profile).equals(fold(passport));
    }

    private static String fold(String name) {
        String stripped = DIACRITICS.matcher(Normalizer.normalize(name, Normalizer.Form.NFD)).replaceAll("");
        String noApostrophes = APOSTROPHES.matcher(stripped).replaceAll("");
        return NAME_SEPARATORS.matcher(noApostrophes.strip()).replaceAll(" ").toUpperCase(Locale.ROOT);
    }
}
