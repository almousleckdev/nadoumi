package com.nadoumi.media.service;

import com.nadoumi.common.media.MediaAccessLogContext;
import com.nadoumi.media.domain.MediaAccessLog;
import com.nadoumi.media.mapper.MediaAccessLogMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Appends one {@code nad_media_access_log} row per PROTECTED / SENSITIVE access
 * attempt. Every write runs in its own {@link Propagation#REQUIRES_NEW}
 * transaction so a later authorization failure in the caller cannot roll back the
 * audit trail (spec §I.6). A dedicated bean by design — an in-class self-call
 * would bypass the new-transaction boundary.
 */
public class MediaAccessLogWriter {

    /** {@code access_kind} for a PROTECTED signed-URL issue. */
    static final String KIND_SIGNED_URL_ISSUED = "SIGNED_URL_ISSUED";

    /** {@code access_kind} for a SENSITIVE / PROTECTED backend proxy read. */
    static final String KIND_STREAM_PROXY = "STREAM_PROXY";

    /** {@code access_kind} for a bare denial with no specific delivery attempt. */
    static final String KIND_METADATA = "METADATA";

    static final String RESULT_GRANTED = "GRANTED";
    static final String RESULT_DENIED = "DENIED";

    private static final int USER_AGENT_MAX = 255;

    private final MediaAccessLogMapper accessLogMapper;

    public MediaAccessLogWriter(MediaAccessLogMapper accessLogMapper) {
        this.accessLogMapper = accessLogMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void granted(long assetId, MediaAccessLogContext ctx, String accessKind, Integer ttlSeconds) {
        write(assetId, ctx, accessKind, RESULT_GRANTED, null, ttlSeconds);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void denied(long assetId, MediaAccessLogContext ctx, String accessKind, String denyReason) {
        write(assetId, ctx, accessKind, RESULT_DENIED, denyReason, null);
    }

    private void write(long assetId, MediaAccessLogContext ctx, String accessKind, String result,
            String denyReason, Integer ttlSeconds) {
        MediaAccessLog row = new MediaAccessLog();
        row.setMediaAssetId(assetId);
        row.setDocumentId(ctx.documentId());
        row.setApplicationId(ctx.applicationId());
        row.setActorUserId(ctx.actorUserId());
        row.setActorApplicantId(ctx.actorApplicantId());
        row.setAccessKind(accessKind);
        row.setResult(result);
        row.setDenyReason(denyReason);
        row.setTtlSeconds(ttlSeconds);
        row.setIp(ctx.ip());
        row.setUserAgent(truncate(ctx.userAgent()));
        accessLogMapper.insert(row);
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= USER_AGENT_MAX) {
            return value;
        }
        return value.substring(0, USER_AGENT_MAX);
    }
}
