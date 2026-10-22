package com.nadoumi.applicant.web.request;

import com.nadoumi.applicant.domain.enums.FamilyRelation;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** One family member. A parent who has died is recorded with {@code deceased} and a note. */
public record FamilyMemberRequest(
        @NotNull
        FamilyRelation relation,
        @NotBlank @Size(max = 150)
        String fullName,
        LocalDate dateOfBirth,
        @Size(max = 60)
        String nationality,
        Boolean deceased,
        @Size(max = 300)
        String note,
        @Size(max = 200)
        String company,
        @Size(max = 150)
        String position,
        @Size(max = 300)
        String companyAddress,
        @Size(max = 32)
        String phone,
        @Email @Size(max = 120)
        String email) {
}
