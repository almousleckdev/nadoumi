package com.nadoumi.identity.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Student self-registration. The student chooses a {@code username} and verifies an {@code email}; their legal
 * names are collected later, in onboarding, from the passport details. The {@code ticket} proves the email was
 * already verified by an OTP.
 */
public record StudentRegisterRequest(
        @NotBlank @Size(max = 40) String username,
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank String password,
        @NotBlank String ticket) {
}
