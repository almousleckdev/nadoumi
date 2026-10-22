package com.nadoumi.identity.service.mail;

import java.util.Arrays;
import java.util.Objects;

/** A file sent with an email. The bytes are copied in so the message stays immutable. */
public record EmailAttachment(String filename, String contentType, byte[] content) {

    public EmailAttachment {
        Objects.requireNonNull(filename, "filename");
        Objects.requireNonNull(contentType, "contentType");
        content = Arrays.copyOf(Objects.requireNonNull(content, "content"), content.length);
    }

    @Override
    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof EmailAttachment a && filename.equals(a.filename) && contentType.equals(a.contentType)
                && Arrays.equals(content, a.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filename, contentType, Arrays.hashCode(content));
    }

    @Override
    public String toString() {
        return "EmailAttachment[" + filename + ", " + content.length + " bytes]";
    }
}
