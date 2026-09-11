package com.nadoumi.identity.service.mail;

import java.time.Instant;

/**
 * Published by the identity layer after an account's password has been changed
 * (self-service reset or an authenticated change). Consumed
 * {@code AFTER_COMMIT} by {@link PasswordChangedMailer} — the security-notice
 * email must never roll back the password change.
 */
public record PasswordChangedEvent(Long userId, String email, Instant changedAt) {

    public PasswordChangedEvent(Long userId, String email) {
        this(userId, email, Instant.now());
    }
}
