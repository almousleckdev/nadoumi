package com.nadoumi.media.spi;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.common.media.MediaAccessClass;
import com.nadoumi.common.media.MediaCategory;
import com.nadoumi.common.media.MediaOwnerKind;
import com.nadoumi.common.media.MediaOwnerRef;
import com.nadoumi.common.media.MediaStorageService;
import com.nadoumi.common.media.MediaUploadCommand;
import com.nadoumi.common.media.StoredAsset;
import com.nadoumi.media.config.MediaProperties;
import com.nadoumi.media.domain.MediaAsset;
import com.nadoumi.media.mapper.MediaAssetMapper;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * What every {@link MediaStorageService} implementation must do the same way: persist a row
 * identical in shape, guard each delivery method by access class, supersede on replace, and
 * soft-delete. The mapper is an in-memory stub, so nothing touches a database.
 */
abstract class MediaStorageContractTest {

    protected final Map<Long, MediaAsset> rows = new HashMap<>();
    protected final MediaAssetMapper assetMapper = mock(MediaAssetMapper.class);
    protected final MediaProperties properties = new MediaProperties();
    private final AtomicLong ids = new AtomicLong();

    protected abstract MediaStorageService storage();

    protected abstract String expectedProvider();

    @BeforeEach
    void stubMapper() {
        properties.setEnv("test");
        doAnswer(call -> {
            MediaAsset row = call.getArgument(0);
            row.setId(ids.incrementAndGet());
            rows.put(row.getId(), row);
            return null;
        }).when(assetMapper).insert(any(MediaAsset.class));
        when(assetMapper.findById(anyLong())).thenAnswer(call -> rows.get(call.<Long>getArgument(0)));
    }

    protected static MediaUploadCommand command(MediaCategory category, MediaAccessClass accessClass) {
        byte[] bytes = "bytes".getBytes(UTF_8);
        return new MediaUploadCommand(new ByteArrayInputStream(bytes), "scan.png", "image/png", bytes.length,
                category, accessClass, new MediaOwnerRef(MediaOwnerKind.APPLICANT, 5L), 7L, "sha");
    }

    @Test
    void shouldPersistAPublicAssetWithAUrl() {
        StoredAsset asset = storage().put(command(MediaCategory.UNIVERSITY_LOGO, null));

        MediaAsset row = rows.get(asset.id());
        assertThat(row.getProvider()).isEqualTo(expectedProvider());
        assertThat(row.getAccessClass()).isEqualTo("PUBLIC");
        assertThat(row.getDeliveryType()).isEqualTo("upload");
        assertThat(row.getCategory()).isEqualTo("UNIVERSITY_LOGO");
        assertThat(row.getSecureUrl()).isNotBlank();
        assertThat(row.getStatus()).isEqualTo("ACTIVE");
        assertThat(row.getOwnerKind()).isEqualTo("APPLICANT");
        assertThat(row.getOwnerId()).isEqualTo(5L);
        assertThat(row.getUploadedBy()).isEqualTo(7L);
        assertThat(row.getChecksumSha256()).isEqualTo("sha");
        assertThat(row.getOriginalFilename()).isEqualTo("scan.png");
        assertThat(row.getCreateBy()).isEqualTo("system");
        assertThat(row.getFolder()).isEqualTo("nadoumi/test/university/logo");
    }

    @Test
    void shouldPersistAProtectedAssetWithNoStoredUrl() {
        StoredAsset asset = storage().put(command(MediaCategory.APPLICANT_PASSPORT, null));

        MediaAsset row = rows.get(asset.id());
        assertThat(row.getAccessClass()).isEqualTo("PROTECTED");
        assertThat(row.getDeliveryType()).isEqualTo("authenticated");
        assertThat(row.getSecureUrl()).isNull();
        assertThat(asset.secureUrl()).isNull();
    }

    @Test
    void shouldServeAPublicUrlOnlyForPublicAssets() {
        long publicId = storage().put(command(MediaCategory.UNIVERSITY_LOGO, null)).id();
        long protectedId = storage().put(command(MediaCategory.APPLICANT_PHOTO, null)).id();

        assertThat(storage().publicUrl(publicId)).isNotBlank();
        assertThatThrownBy(() -> storage().publicUrl(protectedId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("publicUrl is only for PUBLIC assets")
                .hasMessageContaining(String.valueOf(protectedId));
    }

    @Test
    void shouldSignOnlyProtectedAssets() {
        long publicId = storage().put(command(MediaCategory.UNIVERSITY_LOGO, null)).id();

        assertThatThrownBy(() -> storage().signedUrl(publicId, java.time.Duration.ofSeconds(60)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("signedUrl is only for PROTECTED assets");
    }

    @Test
    void shouldStreamOnlyProtectedOrSensitiveAssets() {
        long publicId = storage().put(command(MediaCategory.UNIVERSITY_LOGO, null)).id();

        assertThatThrownBy(() -> storage().openStream(publicId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("openStream is only for PROTECTED/SENSITIVE assets");
    }

    @Test
    void shouldSupersedeTheOldAssetOnReplaceAndKeepItsAccessClass() {
        long oldId = storage().put(command(MediaCategory.APPLICANT_PHOTO, null)).id();

        StoredAsset replacement = storage().replace(oldId, command(MediaCategory.APPLICANT_PHOTO, null));

        verify(assetMapper).updateStatus(oldId, "SUPERSEDED", replacement.id(), "system", null);
        assertThat(rows.get(replacement.id()).getAccessClass()).isEqualTo("PROTECTED");
    }

    @Test
    void shouldFindAnAssetAndReportAMissingOneAsEmpty() {
        long id = storage().put(command(MediaCategory.UNIVERSITY_LOGO, null)).id();

        assertThat(storage().find(id)).isPresent();
        assertThat(storage().find(9999L)).isEmpty();
        assertThatThrownBy(() -> storage().publicUrl(9999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("media asset not found: 9999");
    }

    @Test
    void shouldSoftDeleteByMarkingTheRowDeleted() {
        long id = storage().put(command(MediaCategory.UNIVERSITY_LOGO, null)).id();

        storage().softDelete(id, 42L);

        verify(assetMapper).updateStatus(id, "DELETED", null, "system", 42L);
    }

    @Test
    void shouldRespectAnExplicitAccessClassOverTheCategoryDefault() {
        StoredAsset asset = storage().put(command(MediaCategory.UNIVERSITY_LOGO, MediaAccessClass.PROTECTED));

        assertThat(rows.get(asset.id()).getAccessClass()).isEqualTo("PROTECTED");
        assertThat(rows.get(asset.id()).getSecureUrl()).isNull();
    }
}
