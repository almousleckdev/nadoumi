package com.nadoumi.common.media;

import java.io.InputStream;
import java.util.Optional;

/**
 * Façade that domain modules (university, scholarship, program, applicant,
 * document, ...) call for all media needs, so they depend only on
 * {@code nadoumi-common} and never on {@code nadoumi-media}. Implemented by
 * {@code MediaService} in {@code nadoumi-media}, which layers validation, the
 * category policy, access logging and authorization on top of
 * {@link MediaStorageService}.
 *
 * <p>Signatures use JDK types only — controllers unpack their own multipart input
 * and pass an {@link InputStream}. {@code nadoumi-common} stays Spring-free.</p>
 *
 * <p>Spec: {@code docs/superpowers/specs/2026-09-03-media-storage-and-application-engine-design.md}.</p>
 */
public interface MediaGateway {

    /**
     * Validate and store an upload, persisting the asset row and an access-log
     * entry. {@code accessClassOrNull} falls back to the {@link MediaCategory}
     * default when null.
     */
    MediaUploadResult upload(InputStream source,
                             String originalFilename,
                             String declaredContentType,
                             long byteSize,
                             MediaCategory category,
                             MediaAccessClass accessClassOrNull,
                             MediaOwnerRef owner,
                             long uploadedBy);

    Optional<StoredAsset> find(long assetId);

    /** PUBLIC assets only — the stable delivery URL. Throws for PROTECTED / SENSITIVE. */
    String publicUrl(long assetId);

    /** PROTECTED assets — a short-TTL signed URL; writes an access-log entry from {@code ctx}. */
    SignedUrl issueSignedUrl(long assetId, MediaAccessLogContext ctx);

    /** PROTECTED / SENSITIVE assets — an open stream for backend proxying; writes an access-log entry. */
    ProxyStream openProxyStream(long assetId, MediaAccessLogContext ctx);

    /** Record a denied access attempt with its reason (no bytes are served). */
    void denyAndLog(long assetId, MediaAccessLogContext ctx, String reason);

    void softDelete(long assetId, long actorUserId);
}
