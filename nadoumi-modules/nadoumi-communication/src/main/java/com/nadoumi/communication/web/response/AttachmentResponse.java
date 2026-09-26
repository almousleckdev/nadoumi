package com.nadoumi.communication.web.response;

public record AttachmentResponse(
        long id,
        long mediaAssetId,
        String filename,
        String contentType,
        long byteSize,
        String url) {

    public AttachmentResponse(long id, long mediaAssetId, String filename, String contentType, long byteSize) {
        this(id, mediaAssetId, filename, contentType, byteSize, null);
    }
}
