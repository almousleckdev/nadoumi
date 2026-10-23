package com.nadoumi.applicant.web.response;

import com.nadoumi.applicant.domain.ApplicantPersonal;

public record PersonalDetailsResponse(
        String maritalStatus,
        String religion,
        String placeOfBirth,
        String officialLanguage,
        String permanentAddress,
        String permanentPostcode) {

    public static PersonalDetailsResponse of(ApplicantPersonal p) {
        return new PersonalDetailsResponse(p.getMaritalStatus() == null ? null : p.getMaritalStatus().name(),
                p.getReligion(), p.getPlaceOfBirth(), p.getOfficialLanguage(), p.getPermanentAddress(),
                p.getPermanentPostcode());
    }
}
