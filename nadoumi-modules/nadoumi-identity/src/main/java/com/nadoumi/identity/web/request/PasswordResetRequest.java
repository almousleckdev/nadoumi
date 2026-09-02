package com.nadoumi.identity.web.request;

import jakarta.validation.constraints.NotBlank;

/** Complete a forgotten-password reset with a PASSWORD_RESET ticket. */
public record PasswordResetRequest(
        @NotBlank String ticket,
        @NotBlank String newPassword) {
}
