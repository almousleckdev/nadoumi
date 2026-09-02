package com.nadoumi.applicant.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Staff creating an applicant. {@code invitedEmail} is mandatory: it receives the
 * PENDING owner invite while a staff member holds the interim owner grant
 * (DOMAIN_MODEL §4.3).
 */
public record StaffCreateApplicantRequest(
        @NotBlank @Size(max = 100)
        String givenName,
        @NotBlank @Size(max = 100)
        String familyName,
        LocalDate dob,
        @Size(min = 2, max = 2)
        String nationality,
        @Size(max = 64)
        String passportNo,
        @Email @Size(max = 120)
        String email,
        @Size(max = 32)
        String phone,
        @NotBlank @Email @Size(max = 120)
        String invitedEmail) {
}
