package com.nadoumi.media.spi;

import com.nadoumi.common.media.MediaUploadCommand;
import com.nadoumi.common.media.ProxyStream;
import com.nadoumi.common.media.SignedUrl;
import com.nadoumi.media.config.MediaProperties;
import com.nadoumi.media.domain.MediaAsset;
import com.nadoumi.media.mapper.MediaAssetMapper;
import com.nadoumi.media.policy.MediaCategoryPolicy.CategoryRule;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * No-network {@link MediaStorageService} for local development and demos, used
 * when {@code CLOUDINARY_URL} is not set. Bytes are written under
 * {@code <baseDir>/<folder>/<id>` and served by RuoYi's static handler at
 * {@code <baseUrl>/profile/media/...}. PROTECTED / SENSITIVE assets are handed a
 * plain time-boxed URL / an {@code InputStream} — there is no real signing here,
 * which is acceptable only for local dev.
 *
 * <p>Persistence in {@code nad_media_asset} is identical to the Cloudinary path so
 * every downstream read model behaves the same.</p>
 */
public class LocalFilesystemMediaStorage extends AbstractMediaStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalFilesystemMediaStorage.class);

    private static final String PROVIDER = "LOCAL";
    private static final String URL_PREFIX = "/profile/media/";

    private final Path baseDir;
    private final String baseUrl;

    public LocalFilesystemMediaStorage(MediaAssetMapper assetMapper, MediaProperties properties,
            Path baseDir, String baseUrl) {
        super(assetMapper, properties);
        this.baseDir = baseDir;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        log.warn("CLOUDINARY_URL is not set \u2014 media uploads are stored on the local filesystem at {} "
                + "and served from {}{}. Do NOT use this outside local development.", baseDir, this.baseUrl, URL_PREFIX);
    }

    @Override
    protected String provider() {
        return PROVIDER;
    }

    @Override
    protected StoredObject store(MediaUploadCommand cmd, byte[] bytes, String deliveryType, String folder,
            CategoryRule rule) {
        String publicId = folder + "/" + UUID.randomUUID() + extensionFor(cmd.originalFilename());
        Path target = baseDir.resolve(publicId);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to write local media asset for category " + cmd.category(), e);
        }
        return new StoredObject(publicId, rule.resourceType(), null, null, baseUrl + URL_PREFIX + publicId,
                bytes.length, null, null);
    }

    @Override
    protected SignedUrl signed(MediaAsset row, Duration ttl) {
        return new SignedUrl(baseUrl + URL_PREFIX + row.getPublicId(), Instant.now().plus(ttl));
    }

    @Override
    protected ProxyStream openProviderStream(MediaAsset row) {
        Path file = baseDir.resolve(row.getPublicId());
        try {
            return new ProxyStream(Files.newInputStream(file), row.getContentType(), Files.size(file),
                    row.getOriginalFilename());
        } catch (IOException e) {
            throw new UncheckedIOException("failed to open local media asset " + row.getId() + " at " + file, e);
        }
    }

    @Override
    public void purge(long assetId) {
        MediaAsset row = require(assetId);
        Path file = baseDir.resolve(row.getPublicId());
        try {
            boolean removed = Files.deleteIfExists(file);
            log.info("local media asset purged: id={} path={} removed={}", assetId, file, removed);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to delete local media asset " + assetId + " at " + file, e);
        }
    }

    private static String extensionFor(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot > 0 && dot < filename.length() - 1 ? filename.substring(dot).toLowerCase() : "";
    }
}
