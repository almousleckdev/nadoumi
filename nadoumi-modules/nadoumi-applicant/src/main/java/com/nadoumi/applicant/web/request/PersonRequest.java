package com.nadoumi.applicant.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** The financial supporter, or the emergency contact in China: same details, one of each per applicant. */
public record PersonRequest(
        @NotBlank @Size(max = 150)
        String fullName,
        @NotBlank @Size(max = 80)
        String relationship,
        @Pattern(regexp = "FEMALE|MALE|UNSPECIFIED")
        String gender,
        @Email @Size(max = 120)
        String email,
        @NotBlank @Size(max = 32)
        String phone,
        @Size(max = 150)
        String position,
        @Size(max = 200)
        String company,
        @Size(max = 300)
        String companyAddress,
        @Size(max = 300)
        String homeAddress) {
}
