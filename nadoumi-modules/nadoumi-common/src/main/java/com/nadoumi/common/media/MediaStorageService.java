package com.nadoumi.common.media;

import java.time.Duration;
import java.util.Optional;

/**
 * Provider-agnostic media storage SPI (Part I, D5). Domain modules never reference
 * a Cloudinary type — they depend on this interface from {@code nadoumi-common} and
 * receive the implementation ({@code CloudinaryMediaStorage} in {@code nadoumi-media})
 * by injection.
 *
 * <p>Spec: {@code docs/superpowers/specs/2026-09-03-media-storage-and-application-engine-design.md} §I.2.</p>
 */
public interface MediaStorageService {

    /** Upload already-validated bytes and persist a {@code nad_media_asset} row. */
    StoredAsset put(MediaUploadCommand cmd);

    /**
     * Upload a replacement object for an existing asset; the old asset row is
     * marked SUPERSEDED and retained for audit. Returns the new asset.
     */
    StoredAsset replace(long assetId, MediaUploadCommand cmd);

    /** PUBLIC assets only — the stored secure URL. Throws for PROTECTED / SENSITIVE. */
    String publicUrl(long assetId);

    /**
     * PROTECTED assets only — a freshly signed, short-TTL delivery URL. Throws for
     * PUBLIC (use {@link #publicUrl}) and SENSITIVE (use {@link #openStream}).
     */
    SignedUrl signedUrl(long assetId, Duration ttl);

    /**
     * PROTECTED or SENSITIVE — opens the provider object for backend proxying. The
     * caller is responsible for having authorized the request.
     */
    ProxyStream openStream(long assetId);

    Optional<StoredAsset> find(long assetId);

    /** Soft-delete: row status=DELETED + deleted_at/by; provider destroy is async. */
    void softDelete(long assetId, long actorUserId);
}
