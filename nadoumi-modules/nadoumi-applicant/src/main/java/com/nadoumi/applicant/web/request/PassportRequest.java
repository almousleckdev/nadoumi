package com.nadoumi.applicant.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * The passport's number and validity dates. The holder's names and date of birth are
 * optional: the profile is the identity, so a student is not asked to type them again.
 * When they are sent (staff entry, scan reading) the server compares them with the
 * profile; it does not trust them as identity verification.
 */
public record PassportRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9]{5,20}")
        String passportNo,
        @Size(max = 100)
        String givenName,
        @Size(max = 100)
        String familyName,
        LocalDate dob,
        @NotNull LocalDate issueDate,
        @NotNull LocalDate expiryDate,
        @NotNull @Pattern(regexp = "MRZ|MANUAL")
        String readMethod,
        boolean edited) {
}
