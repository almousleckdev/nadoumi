package com.nadoumi.media.spi;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.nadoumi.common.media.MediaAccessClass;
import com.nadoumi.common.media.MediaCategory;
import com.nadoumi.common.media.MediaOwnerKind;
import com.nadoumi.common.media.MediaOwnerRef;
import com.nadoumi.common.media.MediaUploadCommand;
import com.nadoumi.common.media.SignedUrl;
import com.nadoumi.common.media.StoredAsset;
import com.nadoumi.media.FakeMediaStorage;
import com.nadoumi.media.domain.MediaAsset;
import com.nadoumi.media.mapper.MediaAssetMapper;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Pure unit test — the {@link MediaAssetMapper} is a mock; nothing touches a database. */
class FakeMediaStorageTest {

    private final MediaAssetMapper assetMapper = mock(MediaAssetMapper.class);
    private final FakeMediaStorage storage = new FakeMediaStorage(assetMapper);

    private MediaUploadCommand command(MediaCategory category, byte[] bytes) {
        return new MediaUploadCommand(new ByteArrayInputStream(bytes), "file.png", "image/png",
                bytes.length, category, null, new MediaOwnerRef(MediaOwnerKind.UNIVERSITY, 1L), 7L, null);
    }

    @Test
    void putPersistsRowAndReturnsStoredAsset() {
        StoredAsset publicAsset = storage.put(command(MediaCategory.UNIVERSITY_LOGO, "logo".getBytes(UTF_8)));

        verify(assetMapper).insert(any(MediaAsset.class));
        assertThat(publicAsset.accessClass()).isEqualTo(MediaAccessClass.PUBLIC);
        assertThat(publicAsset.secureUrl()).startsWith("https://fake.local/public/");
        assertThat(publicAsset.checksumSha256()).isNotBlank();

        StoredAsset protectedAsset = storage.put(command(MediaCategory.APPLICANT_PHOTO, "photo".getBytes(UTF_8)));

        assertThat(protectedAsset.accessClass()).isEqualTo(MediaAccessClass.PROTECTED);
        assertThat(protectedAsset.secureUrl()).isNull();
    }

    @Test
    void signedUrlForProtectedHasExpiry() {
        long id = storage.put(command(MediaCategory.APPLICANT_PHOTO, "x".getBytes(UTF_8))).id();
        Instant before = Instant.now();

        SignedUrl signed = storage.signedUrl(id, Duration.ofSeconds(180));

        assertThat(signed.url()).contains("exp=");
        assertThat(signed.expiresAt()).isBetween(before.plusSeconds(170), Instant.now().plusSeconds(190));
    }

    @Test
    void publicUrlThrowsForProtected() {
        long id = storage.put(command(MediaCategory.APPLICANT_PHOTO, "x".getBytes(UTF_8))).id();

        assertThatThrownBy(() -> storage.publicUrl(id)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void signedUrlThrowsForPublic() {
        long id = storage.put(command(MediaCategory.UNIVERSITY_LOGO, "x".getBytes(UTF_8))).id();

        assertThatThrownBy(() -> storage.signedUrl(id, Duration.ofSeconds(180)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void openStreamReturnsStoredBytes() throws Exception {
        byte[] original = "sensitive-bytes".getBytes(UTF_8);
        long id = storage.put(command(MediaCategory.APPLICANT_DOCUMENT, original)).id();

        try (InputStream body = storage.openStream(id).body()) {
            assertThat(body.readAllBytes()).isEqualTo(original);
        }
    }
}
