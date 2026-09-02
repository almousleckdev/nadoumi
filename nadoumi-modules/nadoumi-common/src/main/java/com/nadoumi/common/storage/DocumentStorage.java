package com.nadoumi.common.storage;

import java.io.InputStream;
import java.time.Duration;

/**
 * Object-storage SPI for document and attachment bytes (D5). Bytes never live in
 * MySQL — only {@code storage_key} is persisted. Impls: {@code local} (dev),
 * {@code s3} (S3-compatible), selected by Spring profile.
 */
public interface DocumentStorage {

    void put(String key, InputStream content, String contentType, long sizeBytes);

    InputStream get(String key);

    /** Short-lived pre-signed GET URL a browser can fetch directly. Keep {@code ttl} small. */
    String presignedGetUrl(String key, Duration ttl);

    void delete(String key);

    boolean exists(String key);
}
