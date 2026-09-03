/**
 * Media &amp; file storage. Owns {@code nad_media_asset} / {@code nad_media_access_log};
 * {@code CloudinaryMediaStorage} is the sole production impl of
 * {@link com.nadoumi.common.media.MediaStorageService}. Domain modules depend on the
 * SPI, never on this package's mappers. See
 * {@code docs/superpowers/specs/2026-09-03-media-storage-and-application-engine-design.md}.
 */
package com.nadoumi.media;
