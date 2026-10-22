package com.nadoumi.applicant.web.response;

import com.nadoumi.applicant.domain.ApplicantFamilyMember;
import java.time.LocalDate;

public record FamilyMemberResponse(
        Long id,
        String relation,
        String fullName,
        LocalDate dateOfBirth,
        String nationality,
        boolean deceased,
        String note,
        String company,
        String position,
        String companyAddress,
        String phone,
        String email) {

    public static FamilyMemberResponse of(ApplicantFamilyMember m) {
        return new FamilyMemberResponse(m.getId(), m.getRelation().name(), m.getFullName(), m.getDateOfBirth(),
                m.getNationality(), m.isDeceased(), m.getNote(), m.getCompany(), m.getPosition(),
                m.getCompanyAddress(), m.getPhone(), m.getEmail());
    }
}
