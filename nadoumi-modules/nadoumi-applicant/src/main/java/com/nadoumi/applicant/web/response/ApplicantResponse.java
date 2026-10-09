package com.nadoumi.applicant.web.response;

import com.nadoumi.applicant.domain.Applicant;
import com.nadoumi.common.text.Texts;

/**
 * Applicant on the wire. {@code dob} and {@code passportNo} are masked unless the
 * caller is entitled to unmasked PII (SECURITY §6).
 */
public record ApplicantResponse(
        Long id,
        String givenName,
        String familyName,
        String dob,
        String nationality,
        String passportNo,
        String email,
        String phone,
        String status,
        String createdAt,
        String gender,
        String countryOfOrigin,
        String countryOfResidence,
        String nativeLanguage,
        String wechatId,
        String whatsapp,
        boolean emailVerified,
        boolean onboardingComplete,
        boolean welcomePending,
        String photoUrl) {

    private static final String MASK = "••••";

    public static ApplicantResponse of(Applicant a, boolean includePii) {
        return of(a, includePii, null);
    }

    /** {@code photoUrl} is a signed avatar link, minted only where staff see the person (see {@code AvatarLinks}). */
    public static ApplicantResponse of(Applicant a, boolean includePii, String photoUrl) {
        return new ApplicantResponse(
                a.getId(),
                a.getGivenName(),
                a.getFamilyName(),
                includePii ? Texts.stringOrNull(a.getDob()) : mask(a.getDob()),
                a.getNationality(),
                includePii ? a.getPassportNo() : mask(a.getPassportNo()),
                a.getEmail(),
                a.getPhone(),
                a.getStatus() == null ? null : a.getStatus().name(),
                Texts.stringOrNull(a.getCreateTime()),
                a.getGender(),
                a.getCountryOfOrigin(),
                a.getCountryOfResidence(),
                a.getNativeLanguage(),
                a.getWechatId(),
                a.getWhatsapp(),
                a.getEmailVerifiedAt() != null,
                a.getOnboardedAt() != null,
                a.getOnboardedAt() != null && a.getWelcomedAt() == null,
                photoUrl);
    }

    private static String mask(Object v) {
        return v == null ? null : MASK;
    }
}
