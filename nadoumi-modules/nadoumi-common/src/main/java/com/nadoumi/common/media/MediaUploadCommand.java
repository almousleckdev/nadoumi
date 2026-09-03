package com.nadoumi.common.media;

import java.io.InputStream;

/**
 * Already-validated upload request handed to {@link MediaStorageService#put}.
 * {@code source} is consumed exactly once. {@code accessClass} may be {@code null},
 * in which case the provider applies the {@link MediaCategory} default.
 * {@code checksumSha256} is optional and computed by the caller when required.
 */
public record MediaUploadCommand(
        InputStream source,
        String originalFilename,
        String declaredContentType,
        long byteSize,
        MediaCategory category,
        MediaAccessClass accessClass,
        MediaOwnerRef owner,
        long uploadedBy,
        String checksumSha256) {}
