package com.nadoumi.document.web.response;

import com.nadoumi.document.domain.DocumentVersion;
import java.time.LocalDateTime;

/** A version summary shared by the student's "current version" view and the staff full history. */
public record DocumentVersionResponse(
        long id,
        int versionNo,
        String contentType,
        long sizeBytes,
        LocalDateTime uploadedAt,
        String verificationStatus,
        LocalDateTime verifiedAt) {

    public static DocumentVersionResponse from(DocumentVersion v) {
        return new DocumentVersionResponse(v.getId(), v.getVersionNo(), v.getContentType(), v.getSizeBytes(),
                v.getUploadedAt(), v.getVerificationStatus().name(), v.getVerifiedAt());
    }
}
