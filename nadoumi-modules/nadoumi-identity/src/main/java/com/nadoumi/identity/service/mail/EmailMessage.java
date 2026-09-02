package com.nadoumi.identity.service.mail;

/** A rendered, ready-to-send plain-text email. */
public record EmailMessage(String to, String subject, String body) {
}
