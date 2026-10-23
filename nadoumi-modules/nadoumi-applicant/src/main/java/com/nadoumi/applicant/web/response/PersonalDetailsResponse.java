package com.nadoumi.applicant.web.response;

import com.nadoumi.applicant.domain.ApplicantPersonal;
import com.nadoumi.applicant.domain.enums.MailingChoice;

public record PersonalDetailsResponse(
        String maritalStatus,
        String religion,
        String placeOfBirth,
        String officialLanguage,
        String permanentAddress,
        String permanentPostcode,
        String currentPostcode,
        String mailingChoice,
        String mailingAddress,
        String mailingPostcode) {

    public static PersonalDetailsResponse of(ApplicantPersonal p) {
        return new PersonalDetailsResponse(p.getMaritalStatus() == null ? null : p.getMaritalStatus().name(),
                p.getReligion(), p.getPlaceOfBirth(), p.getOfficialLanguage(), p.getPermanentAddress(),
                p.getPermanentPostcode(), p.getCurrentPostcode(),
                (p.getMailingChoice() == null ? MailingChoice.CURRENT : p.getMailingChoice()).name(),
                p.getMailingAddress(), p.getMailingPostcode());
    }
}
