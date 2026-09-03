package com.nadoumi.media.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.common.media.MediaStorageService;
import com.nadoumi.media.config.MediaProperties;
import com.nadoumi.media.domain.MediaAsset;
import com.nadoumi.media.mapper.MediaAccessLogMapper;
import com.nadoumi.media.mapper.MediaAssetMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Pure unit test — mappers and the storage SPI are mocks; nothing touches a database or Cloudinary. */
class MediaReconciliationJobTest {

    private static final int GRACE_DAYS = 30;

    private final MediaAssetMapper assetMapper = mock(MediaAssetMapper.class);
    private final MediaAccessLogMapper accessLogMapper = mock(MediaAccessLogMapper.class);
    private final MediaStorageService storage = mock(MediaStorageService.class);
    private final MediaProperties properties = new MediaProperties();
    private final MediaReconciliationJob job =
            new MediaReconciliationJob(assetMapper, accessLogMapper, storage, properties);

    private MediaAsset deleted(long id) {
        MediaAsset asset = new MediaAsset();
        asset.setId(id);
        asset.setStatus("DELETED");
        asset.setPublicId("pid-" + id);
        asset.setResourceType("image");
        return asset;
    }

    @Test
    void purgesStaleDeletedThatIsStillReferenced() {
        when(assetMapper.findStaleDeleted(any())).thenReturn(List.of(deleted(7L)));
        when(accessLogMapper.countByAsset(7L)).thenReturn(3);

        job.run();

        verify(storage).purge(7L);
        verify(assetMapper).updateStatus(7L, "PURGED", null, "system", null);
        verify(assetMapper, never()).hardDelete(anyLong());
    }

    @Test
    void hardDeletesStaleDeletedThatIsUnreferenced() {
        when(assetMapper.findStaleDeleted(any())).thenReturn(List.of(deleted(9L)));
        when(accessLogMapper.countByAsset(9L)).thenReturn(0);

        job.run();

        verify(storage).purge(9L);
        verify(assetMapper).hardDelete(9L);
        verify(assetMapper, never()).updateStatus(anyLong(), eq("PURGED"), any(), any(), any());
    }

    @Test
    void ignoresFreshDeleted() {
        when(assetMapper.findStaleDeleted(any())).thenReturn(List.of());

        LocalDateTime lower = LocalDateTime.now().minusDays(GRACE_DAYS).minusMinutes(1);
        job.run();
        LocalDateTime upper = LocalDateTime.now().minusDays(GRACE_DAYS).plusMinutes(1);

        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(assetMapper).findStaleDeleted(cutoff.capture());
        assertThat(cutoff.getValue()).isAfter(lower).isBefore(upper);
        verify(storage, never()).purge(anyLong());
        verify(assetMapper, never()).hardDelete(anyLong());
        verify(assetMapper, never()).updateStatus(anyLong(), any(), any(), any(), any());
    }
}
