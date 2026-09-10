package com.nadoumi.identity.service.mail;

/**
 * A rendered, ready-to-send email. {@code body} is always the plain-text
 * alternative; {@code htmlBody} is {@code null} for text-only messages and a full
 * HTML document otherwise (the transport then sends {@code multipart/alternative}).
 */
public record EmailMessage(String to, String subject, String body, String htmlBody) {

    /** A text-only message (ops diagnostics, callers that have no HTML part). */
    public static EmailMessage text(String to, String subject, String body) {
        return new EmailMessage(to, subject, body, null);
    }
}
