package com.nadoumi.applicant.web.response;

import com.nadoumi.applicant.domain.Applicant;

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
        String createdAt) {

    private static final String MASK = "••••";

    public static ApplicantResponse of(Applicant a, boolean includePii) {
        return new ApplicantResponse(
                a.getId(),
                a.getGivenName(),
                a.getFamilyName(),
                includePii ? str(a.getDob()) : mask(a.getDob()),
                a.getNationality(),
                includePii ? a.getPassportNo() : mask(a.getPassportNo()),
                a.getEmail(),
                a.getPhone(),
                a.getStatus() == null ? null : a.getStatus().name(),
                str(a.getCreateTime()));
    }

    private static String str(Object v) {
        return v == null ? null : v.toString();
    }

    private static String mask(Object v) {
        return v == null ? null : MASK;
    }
}
