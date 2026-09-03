package com.nadoumi.university.domain;

/**
 * One image in a university's gallery. {@code mediaId} points at
 * {@code nad_media_asset} once uploaded through the MediaGateway; {@code imageUrl}
 * is the resolved public URL (also kept as the legacy fallback for the
 * deprecation window).
 */
public record UniversityGalleryImage(Long id, String imageUrl, Long mediaId, String caption) {
}
