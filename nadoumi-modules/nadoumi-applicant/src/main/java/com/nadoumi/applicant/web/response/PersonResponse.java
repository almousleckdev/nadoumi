package com.nadoumi.applicant.web.response;

import com.nadoumi.applicant.domain.ApplicantPerson;

public record PersonResponse(
        String role,
        String fullName,
        String relationship,
        String gender,
        String email,
        String phone,
        String position,
        String company,
        String companyAddress,
        String homeAddress) {

    public static PersonResponse of(ApplicantPerson p) {
        return new PersonResponse(p.getRole().name(), p.getFullName(), p.getRelationship(), p.getGender(),
                p.getEmail(), p.getPhone(), p.getPosition(), p.getCompany(), p.getCompanyAddress(), p.getHomeAddress());
    }
}
