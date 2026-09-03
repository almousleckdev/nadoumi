package com.nadoumi.common.media;

import java.time.Instant;

/** A freshly signed, short-lived delivery URL and the instant it stops working. */
public record SignedUrl(String url, Instant expiresAt) {}
