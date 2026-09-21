package com.nadoumi.identity.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Confirm a new sign-in email with its code, and the current password. */
public record EmailChangeRequest(
        @NotBlank @Email @Size(max = 120) String newEmail,
        @NotBlank @Pattern(regexp = "\\d{6}") String otp,
        @NotBlank String currentPassword) {
}
