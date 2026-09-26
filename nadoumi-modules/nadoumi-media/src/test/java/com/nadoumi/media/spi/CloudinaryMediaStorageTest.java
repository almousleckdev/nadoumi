package com.nadoumi.media.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.nadoumi.common.media.MediaCategory;
import com.nadoumi.common.media.MediaStorageService;
import com.nadoumi.common.media.SignedUrl;
import com.nadoumi.common.media.StoredAsset;
import com.nadoumi.media.domain.MediaAsset;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CloudinaryMediaStorageTest extends MediaStorageContractTest {

    private final Cloudinary cloudinary = spy(new Cloudinary("cloudinary://key:secret@cloud"));
    private final Uploader uploader = mock(Uploader.class);
    private CloudinaryMediaStorage storage;

    @BeforeEach
    void createStorage() throws IOException {
        doReturn(uploader).when(cloudinary).uploader();
        when(uploader.upload(any(), anyMap())).thenAnswer(call -> {
            Map<?, ?> options = call.getArgument(1);
            Map<String, Object> result = new HashMap<>();
            result.put("public_id", options.get("folder") + "/abc123");
            result.put("resource_type", options.get("resource_type"));
            result.put("asset_id", "provider-asset");
            result.put("version", 17L);
            result.put("bytes", 5L);
            result.put("width", 640);
            result.put("height", 480);
            result.put("secure_url", "https://res.cloudinary.com/cloud/abc123.png");
            return result;
        });
        storage = new CloudinaryMediaStorage(cloudinary, assetMapper, properties);
    }

    @Override
    protected MediaStorageService storage() {
        return storage;
    }

    @Override
    protected String expectedProvider() {
        return "CLOUDINARY";
    }

    @Test
    void shouldUploadPublicAssetsAsUploadTypeAndProtectedOnesAsAuthenticated() throws IOException {
        storage.put(command(MediaCategory.UNIVERSITY_LOGO, null));
        storage.put(command(MediaCategory.APPLICANT_PASSPORT, null));

        ArgumentCaptor<Map<String, Object>> options = ArgumentCaptor.forClass(Map.class);
        verify(uploader, org.mockito.Mockito.times(2)).upload(any(), options.capture());
        assertThat(options.getAllValues().get(0))
                .containsEntry("type", "upload").containsEntry("folder", "nadoumi/test/university/logo");
        assertThat(options.getAllValues().get(1))
                .containsEntry("type", "authenticated").containsEntry("resource_type", "raw")
                .containsEntry("folder", "nadoumi/test/applicant/passport");
    }

    @Test
    void shouldRecordWhatTheProviderReported() {
        StoredAsset asset = storage.put(command(MediaCategory.UNIVERSITY_LOGO, null));

        MediaAsset row = rows.get(asset.id());
        assertThat(row.getPublicId()).isEqualTo("nadoumi/test/university/logo/abc123");
        assertThat(row.getAssetId()).isEqualTo("provider-asset");
        assertThat(row.getCloudVersion()).isEqualTo(17L);
        assertThat(row.getByteSize()).isEqualTo(5L);
        assertThat(row.getWidth()).isEqualTo(640);
        assertThat(row.getHeight()).isEqualTo(480);
        assertThat(row.getSecureUrl()).isEqualTo("https://res.cloudinary.com/cloud/abc123.png");
    }

    @Test
    void shouldSignAProtectedAssetsDownloadWithAnExpiry() throws Exception {
        long id = storage.put(command(MediaCategory.APPLICANT_PHOTO, null)).id();
        doReturn("https://api.cloudinary.com/download?sig=1").when(cloudinary)
                .privateDownload(any(), any(), anyMap());
        Instant before = Instant.now();

        SignedUrl signed = storage.signedUrl(id, Duration.ofSeconds(180));

        assertThat(signed.url()).isEqualTo("https://api.cloudinary.com/download?sig=1");
        assertThat(signed.expiresAt()).isBetween(before.plusSeconds(170), Instant.now().plusSeconds(190));
        ArgumentCaptor<Map<String, Object>> options = ArgumentCaptor.forClass(Map.class);
        verify(cloudinary).privateDownload(eq(rows.get(id).getPublicId()), any(), options.capture());
        assertThat(options.getValue()).containsEntry("type", "authenticated").containsKey("expires_at");
    }

    @Test
    void shouldMintAnInlineUrlThatDoesNotGoThroughTheForcedDownloadEndpoint() throws Exception {
        long id = storage.put(command(MediaCategory.APPLICANT_PHOTO, null)).id();

        SignedUrl signed = storage.inlineSignedUrl(id, Duration.ofSeconds(180));

        assertThat(signed.url())
                .as("inline delivery must hit the CDN, never the Admin API /download endpoint that forces attachment")
                .doesNotContain("api.cloudinary.com")
                .contains("/authenticated/")
                .contains(rows.get(id).getPublicId());
        verify(cloudinary, org.mockito.Mockito.never()).privateDownload(any(), any(), anyMap());
    }

    @Test
    void shouldDestroyTheProviderObjectOnPurge() throws IOException {
        long id = storage.put(command(MediaCategory.APPLICANT_PHOTO, null)).id();
        when(uploader.destroy(any(), anyMap())).thenReturn(Map.of("result", "ok"));

        storage.purge(id);

        ArgumentCaptor<Map<String, Object>> options = ArgumentCaptor.forClass(Map.class);
        verify(uploader).destroy(eq(rows.get(id).getPublicId()), options.capture());
        assertThat(options.getValue()).containsEntry("type", "authenticated").containsEntry("invalidate", true);
    }
}
