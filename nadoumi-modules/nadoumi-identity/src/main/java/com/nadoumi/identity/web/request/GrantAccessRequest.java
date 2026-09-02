package com.nadoumi.identity.web.request;

import com.nadoumi.common.access.AccessRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/** Delegate a grant: supply {@code userId} for an existing user, or {@code email} for an invite. */
public record GrantAccessRequest(
        Long userId,
        @Email String email,
        @NotNull AccessRole role,
        LocalDateTime expiresAt) {
}
