package com.nadoumi.identity.service.mail;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

/**
 * One outbound email. {@code attachments} are never serialised (the dev mail endpoint and the log outbox show
 * their names only), and the SMTP sender refuses a message whose attachments exceed {@link #MAX_ATTACHMENT_BYTES}.
 */
public record EmailMessage(String to, String subject, String body, String htmlBody,
        @JsonIgnore List<EmailAttachment> attachments) {

    /** Total attachment size a single message may carry. */
    public static final int MAX_ATTACHMENT_BYTES = 10 * 1024 * 1024;

    public EmailMessage {
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }

    public EmailMessage(String to, String subject, String body, String htmlBody) {
        this(to, subject, body, htmlBody, List.of());
    }

    /** A text-only message (ops diagnostics, callers that have no HTML part). */
    public static EmailMessage text(String to, String subject, String body) {
        return new EmailMessage(to, subject, body, null);
    }

    public EmailMessage withAttachment(EmailAttachment attachment) {
        return new EmailMessage(to, subject, body, htmlBody,
                java.util.stream.Stream.concat(attachments.stream(), java.util.stream.Stream.of(attachment)).toList());
    }

    public List<String> attachmentNames() {
        return attachments.stream().map(EmailAttachment::filename).toList();
    }

    public long attachmentBytes() {
        return attachments.stream().mapToLong(a -> a.content().length).sum();
    }
}
