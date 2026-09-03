package com.nadoumi.common.media;

/**
 * Who asked for a protected asset and in what request context. Written to
 * {@code nad_media_access_log} on every signed-URL issue, proxy read and denial
 * so protected/sensitive delivery is auditable. Nullable fields are absent when
 * the dimension does not apply (e.g. no applicant or document in scope).
 */
public record MediaAccessLogContext(
        long actorUserId,
        Long actorApplicantId,
        Long applicationId,
        Long documentId,
        String ip,
        String userAgent) {}
