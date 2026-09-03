package com.nadoumi.common.media;

/**
 * Outcome of {@link MediaGateway#upload}. {@code url} is the ready-to-use public URL
 * for {@link MediaAccessClass#PUBLIC} assets and {@code null} for PROTECTED / SENSITIVE
 * assets, which are delivered through signed URLs or a backend proxy instead.
 */
public record MediaUploadResult(long mediaId, String url) {}
