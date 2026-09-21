package com.nadoumi.communication.web.response;

public record AttachmentResponse(long id, long mediaAssetId, String filename, String contentType, long byteSize) {
}
