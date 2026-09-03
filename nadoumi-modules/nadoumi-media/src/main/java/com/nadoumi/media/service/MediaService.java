package com.nadoumi.media.service;

import com.nadoumi.common.media.MediaAccessClass;
import com.nadoumi.common.media.MediaAccessLogContext;
import com.nadoumi.common.media.MediaCategory;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.common.media.MediaOwnerRef;
import com.nadoumi.common.media.MediaStorageService;
import com.nadoumi.common.media.MediaUploadCommand;
import com.nadoumi.common.media.MediaUploadResult;
import com.nadoumi.common.media.ProxyStream;
import com.nadoumi.common.media.SignedUrl;
import com.nadoumi.common.media.StoredAsset;
import com.nadoumi.media.NadMediaNotFoundException;
import com.nadoumi.media.config.MediaProperties;
import com.nadoumi.media.policy.MediaCategoryPolicy;
import com.nadoumi.media.spi.MediaChecksums;
import com.nadoumi.media.validation.MediaValidation;
import com.nadoumi.media.validation.MediaValidation.ValidatedUpload;
import com.nadoumi.media.validation.MediaValidationException;
import com.nadoumi.media.validation.MediaValidationException.Reason;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/**
 * The {@link MediaGateway} implementation domain modules call. Layers the upload
 * boundary ({@link MediaValidation}), the category policy, access-class
 * resolution, SHA-256 checksumming and the access-log audit on top of the
 * provider SPI ({@link MediaStorageService}). Domain authorization stays with the
 * owning module; this class assumes the caller has already authorized the
 * request and records what was served.
 *
 * <p><b>Single-consume streams.</b> {@link MediaValidation#check} drains up to
 * 8&nbsp;KiB of the stream it is given with no mark/reset, so {@link #upload}
 * buffers the whole payload into a {@code byte[]} once (the size is already
 * capped by the multipart layer and re-checked here), then validates, checksums
 * and uploads from independent views of that array.</p>
 */
public class MediaService implements MediaGateway {

    private final MediaStorageService storage;
    private final MediaAccessLogWriter accessLog;
    private final MediaProperties properties;

    public MediaService(MediaStorageService storage, MediaAccessLogWriter accessLog, MediaProperties properties) {
        this.storage = storage;
        this.accessLog = accessLog;
        this.properties = properties;
    }

    @Override
    @Transactional
    public MediaUploadResult upload(InputStream source,
                                   String originalFilename,
                                   String declaredContentType,
                                   long byteSize,
                                   MediaCategory category,
                                   MediaAccessClass accessClassOrNull,
                                   MediaOwnerRef owner,
                                   long uploadedBy) {
        long maxBytes = properties.getMaxUploadBytes();
        if (byteSize > 0 && byteSize > maxBytes) {
            throw tooLarge(byteSize, maxBytes);
        }

        byte[] bytes = readAll(source);
        if (bytes.length > maxBytes) {
            throw tooLarge(bytes.length, maxBytes);
        }

        ValidatedUpload validated = MediaValidation.check(new ByteArrayInputStream(bytes),
                declaredContentType, originalFilename, bytes.length, category);

        MediaAccessClass accessClass = accessClassOrNull != null
                ? accessClassOrNull
                : MediaCategoryPolicy.of(category).defaultAccessClass();

        MediaUploadCommand cmd = new MediaUploadCommand(
                new ByteArrayInputStream(bytes),
                validated.sanitizedFilename(),
                validated.resolvedContentType(),
                bytes.length,
                category,
                accessClass,
                owner,
                uploadedBy,
                MediaChecksums.sha256Hex(bytes));

        StoredAsset asset = storage.put(cmd);
        String url = asset.accessClass() == MediaAccessClass.PUBLIC ? asset.secureUrl() : null;
        return new MediaUploadResult(asset.id(), url);
    }

    @Override
    public Optional<StoredAsset> find(long assetId) {
        return storage.find(assetId);
    }

    @Override
    public String publicUrl(long assetId) {
        return storage.publicUrl(assetId);
    }

    @Override
    public SignedUrl issueSignedUrl(long assetId, MediaAccessLogContext ctx) {
        StoredAsset asset = require(assetId);
        if (asset.accessClass() != MediaAccessClass.PROTECTED) {
            throw new IllegalArgumentException("issueSignedUrl is only for PROTECTED assets; media asset "
                    + assetId + " is " + asset.accessClass());
        }
        int ttlSeconds = properties.getSignedUrlTtlSeconds();
        accessLog.granted(assetId, ctx, MediaAccessLogWriter.KIND_SIGNED_URL_ISSUED, ttlSeconds);
        return storage.signedUrl(assetId, Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public ProxyStream openProxyStream(long assetId, MediaAccessLogContext ctx) {
        StoredAsset asset = require(assetId);
        MediaAccessClass accessClass = asset.accessClass();
        if (accessClass != MediaAccessClass.PROTECTED && accessClass != MediaAccessClass.SENSITIVE) {
            throw new IllegalArgumentException("openProxyStream is only for PROTECTED/SENSITIVE assets; media asset "
                    + assetId + " is " + accessClass);
        }
        accessLog.granted(assetId, ctx, MediaAccessLogWriter.KIND_STREAM_PROXY, null);
        return storage.openStream(assetId);
    }

    @Override
    public void denyAndLog(long assetId, MediaAccessLogContext ctx, String reason) {
        accessLog.denied(assetId, ctx, MediaAccessLogWriter.KIND_METADATA, reason);
    }

    @Override
    @Transactional
    public void softDelete(long assetId, long actorUserId) {
        storage.softDelete(assetId, actorUserId);
    }

    private StoredAsset require(long assetId) {
        return storage.find(assetId)
                .orElseThrow(() -> new NadMediaNotFoundException("media asset not found: " + assetId));
    }

    private static MediaValidationException tooLarge(long actual, long max) {
        return new MediaValidationException(Reason.TOO_LARGE,
                "upload is " + actual + " bytes; the hard ceiling is " + max);
    }

    private static byte[] readAll(InputStream source) {
        try (InputStream in = source) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read media upload", e);
        }
    }
}
