package com.nadoumi.common.media;

import java.io.InputStream;

/**
 * An open provider object for backend proxying of PROTECTED / SENSITIVE bytes.
 * The caller owns closing {@code body} and has already authorized the request.
 */
public record ProxyStream(
        InputStream body,
        String contentType,
        long contentLength,
        String downloadFilename) {}
