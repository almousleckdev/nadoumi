package com.nadoumi.applicant.web.response;

import com.nadoumi.applicant.domain.Applicant;
import com.nadoumi.applicant.rules.PassportRules;
import java.time.LocalDate;
import java.util.List;

/**
 * Where the applicant's passport stands: the saved details, whether the scan exists,
 * whether it is valid for long enough, and which profile fields disagree with it.
 * The passport number and dates of birth are masked unless the caller may see PII.
 */
public record PassportStatusResponse(
        String passportNo,
        String givenName,
        String familyName,
        String dob,
        String issueDate,
        String expiryDate,
        String readMethod,
        boolean edited,
        boolean scanUploaded,
        boolean validForAdmission,
        boolean matchesProfile,
        List<Mismatch> mismatches) {

    private static final String MASK = "••••";

    /** One profile field that disagrees with the passport, with both values (masked without PII). */
    public record Mismatch(String field, String passportValue, String profileValue) {
    }

    public static PassportStatusResponse of(Applicant a, LocalDate today, boolean includePii) {
        boolean hasDetails = a.getPassportGivenName() != null;
        List<Mismatch> mismatches = hasDetails ? mismatchesOf(a, includePii) : List.of();
        return new PassportStatusResponse(
                includePii ? a.getPassportNo() : mask(a.getPassportNo()),
                a.getPassportGivenName(),
                a.getPassportFamilyName(),
                includePii ? str(a.getPassportDob()) : mask(a.getPassportDob()),
                str(a.getPassportIssueDate()),
                str(a.getPassportExpiryDate()),
                a.getPassportReadMethod(),
                a.isPassportDataEdited(),
                a.getPassportMediaId() != null,
                PassportRules.isValidForAdmission(a.getPassportExpiryDate(), today),
                hasDetails && mismatches.isEmpty(),
                mismatches);
    }

    private static List<Mismatch> mismatchesOf(Applicant a, boolean includePii) {
        return PassportRules.mismatches(a.getGivenName(), a.getFamilyName(), a.getDob(),
                a.getPassportGivenName(), a.getPassportFamilyName(), a.getPassportDob())
                .stream()
                .map(field -> switch (field) {
                    case PassportRules.GIVEN_NAME -> new Mismatch(field, a.getPassportGivenName(), a.getGivenName());
                    case PassportRules.FAMILY_NAME -> new Mismatch(field, a.getPassportFamilyName(), a.getFamilyName());
                    default -> new Mismatch(field,
                            includePii ? str(a.getPassportDob()) : mask(a.getPassportDob()),
                            includePii ? str(a.getDob()) : mask(a.getDob()));
                })
                .toList();
    }

    private static String str(Object value) {
        return value == null ? null : value.toString();
    }

    private static String mask(Object value) {
        return value == null ? null : MASK;
    }
}
