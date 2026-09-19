package com.nadoumi.applicant.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * The passport's holder data as read from, or typed off, the passport. The server
 * compares it with the profile; it does not trust it as identity verification.
 */
public record PassportRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9]{5,20}")
        String passportNo,
        @NotBlank @Size(max = 100)
        String givenName,
        @NotBlank @Size(max = 100)
        String familyName,
        @NotNull LocalDate dob,
        @NotNull LocalDate issueDate,
        @NotNull LocalDate expiryDate,
        @NotNull @Pattern(regexp = "MRZ|MANUAL")
        String readMethod,
        boolean edited) {
}
