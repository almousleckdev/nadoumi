package com.nadoumi.identity.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Student self-registration (spec Revision 2, D-R2-2). Identity is email-first:
 * {@code user_name} is generated server-side, and the {@code ticket} proves the
 * email was already verified by an OTP.
 */
public record StudentRegisterRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank String password,
        @NotBlank String ticket) {
}
