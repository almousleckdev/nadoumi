package com.nadoumi.communication.web.response;

/** A short-lived signed URL for a PROTECTED message attachment, plus its display metadata. */
public record AttachmentAccessResponse(String url, String expiresAt, String filename, String contentType) {
}
