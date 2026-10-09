package com.nadoumi.communication.web.response;

/**
 * Display metadata only. The bytes are fetched on demand through the conversation's attachment endpoint, which
 * re-checks participation and hands out a short-lived signed URL, so a listing never signs anything.
 */
public record AttachmentResponse(long id, String filename, String contentType, long byteSize, boolean image) {
}
