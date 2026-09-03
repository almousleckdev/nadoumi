package com.nadoumi.media.spi;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA-256 helper for media uploads. The checksum is computed once over the fully
 * buffered upload bytes (spec §I.5 step 7) and persisted on
 * {@code nad_media_asset.checksum_sha256} for dedupe and tamper evidence.
 */
public final class MediaChecksums {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private MediaChecksums() {
    }

    /** Lower-case hex SHA-256 of {@code bytes}. */
    public static String sha256Hex(byte[] bytes) {
        byte[] digest;
        try {
            digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available in this JVM", e);
        }
        StringBuilder out = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            out.append(HEX[(b >> 4) & 0xF]).append(HEX[b & 0xF]);
        }
        return out.toString();
    }
}
