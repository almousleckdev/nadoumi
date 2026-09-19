package com.nadoumi.media.spi;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.nadoumi.common.media.MediaCategory;
import com.nadoumi.common.media.MediaStorageService;
import com.nadoumi.common.media.ProxyStream;
import com.nadoumi.common.media.SignedUrl;
import com.nadoumi.common.media.StoredAsset;
import com.nadoumi.media.domain.MediaAsset;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFilesystemMediaStorageTest extends MediaStorageContractTest {

    @TempDir
    Path baseDir;

    private LocalFilesystemMediaStorage storage;

    @BeforeEach
    void createStorage() {
        storage = new LocalFilesystemMediaStorage(assetMapper, properties, baseDir, "http://localhost:8080/");
    }

    @Override
    protected MediaStorageService storage() {
        return storage;
    }

    @Override
    protected String expectedProvider() {
        return "LOCAL";
    }

    @Test
    void shouldWriteTheBytesUnderTheFolderAndServeThemFromTheProfilePrefix() throws IOException {
        StoredAsset asset = storage.put(command(MediaCategory.UNIVERSITY_LOGO, null));

        MediaAsset row = rows.get(asset.id());
        assertThat(row.getPublicId()).startsWith("nadoumi/test/university/logo/").endsWith(".png");
        assertThat(Files.readString(baseDir.resolve(row.getPublicId()), UTF_8)).isEqualTo("bytes");
        assertThat(row.getSecureUrl()).isEqualTo("http://localhost:8080/profile/media/" + row.getPublicId());
        assertThat(row.getByteSize()).isEqualTo(5L);
    }

    @Test
    void shouldIssueATimeBoxedUrlForAProtectedAsset() {
        long id = storage.put(command(MediaCategory.APPLICANT_PHOTO, null)).id();
        Instant before = Instant.now();

        SignedUrl signed = storage.signedUrl(id, Duration.ofSeconds(180));

        assertThat(signed.url()).startsWith("http://localhost:8080/profile/media/nadoumi/test/applicant/photo/");
        assertThat(signed.expiresAt()).isBetween(before.plusSeconds(170), Instant.now().plusSeconds(190));
    }

    @Test
    void shouldStreamAProtectedAssetsBytes() throws IOException {
        long id = storage.put(command(MediaCategory.APPLICANT_PHOTO, null)).id();

        ProxyStream stream = storage.openStream(id);
        try (var body = stream.body()) {
            assertThat(body.readAllBytes()).isEqualTo("bytes".getBytes(UTF_8));
        }
        assertThat(stream.contentType()).isEqualTo("image/png");
        assertThat(stream.contentLength()).isEqualTo(5L);
        assertThat(stream.downloadFilename()).isEqualTo("scan.png");
    }

    @Test
    void shouldDeleteTheFileOnPurge() {
        long id = storage.put(command(MediaCategory.UNIVERSITY_LOGO, null)).id();
        Path file = baseDir.resolve(rows.get(id).getPublicId());
        assertThat(file).exists();

        storage.purge(id);

        assertThat(file).doesNotExist();
    }
}
