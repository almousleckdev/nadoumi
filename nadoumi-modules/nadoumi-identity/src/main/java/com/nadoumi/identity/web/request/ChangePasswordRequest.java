package com.nadoumi.identity.web.request;

import jakarta.validation.constraints.NotBlank;

/** Signed-in student changing their own password. */
public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank String newPassword) {
}
