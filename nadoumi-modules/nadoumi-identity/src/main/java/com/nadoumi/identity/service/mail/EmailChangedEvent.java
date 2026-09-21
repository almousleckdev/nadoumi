package com.nadoumi.identity.service.mail;

import java.time.Instant;

/**
 * Published by the identity layer after an account's sign-in email has been
 * changed. {@code notifyEmail} is the <em>previous</em> address — the new one
 * already received the OTP that proved ownership. Consumed {@code AFTER_COMMIT}
 * by {@link EmailChangedMailer} — the security-notice email must never roll back
 * the email change.
 */
public record EmailChangedEvent(Long userId, String notifyEmail, String newEmail, Instant changedAt) {

    public EmailChangedEvent(Long userId, String notifyEmail, String newEmail) {
        this(userId, notifyEmail, newEmail, Instant.now());
    }
}
