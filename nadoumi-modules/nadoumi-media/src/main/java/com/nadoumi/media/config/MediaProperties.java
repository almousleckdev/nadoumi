package com.nadoumi.media.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code nadoumi.media.*} (spec §I.10). {@link #getSignedUrlTtlSeconds()}
 * clamps the configured value to [{@value #MIN_TTL}, {@value #MAX_TTL}] seconds so
 * a mis-set environment variable can never widen the signed-URL window.
 *
 * <p>{@code CLOUDINARY_URL} is deliberately <em>not</em> bound here — it stays a
 * pure environment variable read in {@code MediaAutoConfiguration} so a missing
 * credential fails the context fast.</p>
 */
@ConfigurationProperties("nadoumi.media")
public class MediaProperties {

    /** Lower bound for the signed-URL TTL, in seconds (spec §I.10). */
    public static final int MIN_TTL = 60;

    /** Upper bound for the signed-URL TTL, in seconds (spec §I.10). */
    public static final int MAX_TTL = 600;

    private static final int DEFAULT_TTL_SECONDS = 180;
    private static final int DEFAULT_MAX_UPLOAD_MB = 20;
    private static final int DEFAULT_RECONCILE_GRACE_DAYS = 30;
    private static final long BYTES_PER_MB = 1024L * 1024L;

    private String env = "dev";
    private int signedUrlTtlSeconds = DEFAULT_TTL_SECONDS;
    private int maxUploadMb = DEFAULT_MAX_UPLOAD_MB;
    private int reconcileGraceDays = DEFAULT_RECONCILE_GRACE_DAYS;

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    /** Configured TTL, clamped to [{@value #MIN_TTL}, {@value #MAX_TTL}] seconds. */
    public int getSignedUrlTtlSeconds() {
        return Math.min(MAX_TTL, Math.max(MIN_TTL, signedUrlTtlSeconds));
    }

    public void setSignedUrlTtlSeconds(int signedUrlTtlSeconds) {
        this.signedUrlTtlSeconds = signedUrlTtlSeconds;
    }

    public int getMaxUploadMb() {
        return maxUploadMb;
    }

    public void setMaxUploadMb(int maxUploadMb) {
        this.maxUploadMb = maxUploadMb;
    }

    /** {@link #getMaxUploadMb()} expressed as a byte count. */
    public long getMaxUploadBytes() {
        return (long) maxUploadMb * BYTES_PER_MB;
    }

    public int getReconcileGraceDays() {
        return reconcileGraceDays;
    }

    public void setReconcileGraceDays(int reconcileGraceDays) {
        this.reconcileGraceDays = reconcileGraceDays;
    }
}
