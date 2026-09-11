package com.nadoumi.identity.service.mail;

/** The output of {@link EmailLayout}: a full HTML document and its plain-text twin. */
public record EmailRender(String html, String text) {
}
