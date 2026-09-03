package com.nadoumi.media.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.common.media.MediaAccessClass;
import com.nadoumi.common.media.MediaAccessLogContext;
import com.nadoumi.common.media.MediaCategory;
import com.nadoumi.common.media.MediaOwnerKind;
import com.nadoumi.common.media.MediaOwnerRef;
import com.nadoumi.common.media.MediaStorageService;
import com.nadoumi.common.media.MediaUploadCommand;
import com.nadoumi.common.media.MediaUploadResult;
import com.nadoumi.common.media.ProxyStream;
import com.nadoumi.common.media.SignedUrl;
import com.nadoumi.common.media.StoredAsset;
import com.nadoumi.media.config.MediaProperties;
import com.nadoumi.media.domain.MediaAccessLog;
import com.nadoumi.media.mapper.MediaAccessLogMapper;
import com.nadoumi.media.validation.MediaValidationException;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Pure unit test — every collaborator is a mock; nothing touches a database or Cloudinary. */
class MediaServiceTest {

    /** A real 1×1 PNG so the Tika magic-byte sniff in {@code MediaValidation} passes. */
    private static final byte[] PNG_1X1 = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");

    private final MediaStorageService storage = mock(MediaStorageService.class);
    private final MediaAccessLogMapper accessLogMapper = mock(MediaAccessLogMapper.class);
    private final MediaProperties properties = new MediaProperties();
    private final MediaService service =
            new MediaService(storage, new MediaAccessLogWriter(accessLogMapper), properties);

    private MediaAccessLogContext ctx() {
        return new MediaAccessLogContext(42L, null, null, null, "1.2.3.4", "JUnit");
    }

    private StoredAsset asset(long id, MediaAccessClass accessClass) {
        return new StoredAsset(id, "FAKE", accessClass, MediaCategory.APPLICANT_PHOTO, "image", "authenticated",
                "pid-" + id, "aid-" + id, 1L,
                accessClass == MediaAccessClass.PUBLIC ? "https://cdn.example/x" : null,
                "f.png", "image/png", 10L, null, null, null, 7L,
                new MediaOwnerRef(MediaOwnerKind.APPLICANT, 3L), "ACTIVE", Instant.now());
    }

    @Test
    void uploadValidatesThenPuts() {
        assertThatThrownBy(() -> service.upload(new ByteArrayInputStream("not-an-image".getBytes(UTF_8)),
                "logo.png", "image/png", 12L, MediaCategory.UNIVERSITY_LOGO, null,
                new MediaOwnerRef(MediaOwnerKind.UNIVERSITY, 1L), 7L))
                .isInstanceOf(MediaValidationException.class);

        verify(storage, never()).put(any());
    }

    @Test
    void uploadResolvesDefaultAccessClass() {
        when(storage.put(any())).thenReturn(asset(9L, MediaAccessClass.PUBLIC));

        MediaUploadResult result = service.upload(new ByteArrayInputStream(PNG_1X1), "logo.png", "image/png",
                PNG_1X1.length, MediaCategory.UNIVERSITY_LOGO, null,
                new MediaOwnerRef(MediaOwnerKind.UNIVERSITY, 1L), 7L);

        ArgumentCaptor<MediaUploadCommand> cmd = ArgumentCaptor.forClass(MediaUploadCommand.class);
        verify(storage).put(cmd.capture());
        assertThat(cmd.getValue().accessClass()).isEqualTo(MediaAccessClass.PUBLIC);
        assertThat(cmd.getValue().category()).isEqualTo(MediaCategory.UNIVERSITY_LOGO);
        assertThat(cmd.getValue().checksumSha256()).isNotBlank();
        assertThat(result.mediaId()).isEqualTo(9L);
        assertThat(result.url()).isEqualTo("https://cdn.example/x");
    }

    @Test
    void issueSignedUrlWritesGrantedLog() {
        when(storage.find(5L)).thenReturn(Optional.of(asset(5L, MediaAccessClass.PROTECTED)));
        when(storage.signedUrl(eq(5L), any()))
                .thenReturn(new SignedUrl("https://signed", Instant.now().plusSeconds(180)));

        service.issueSignedUrl(5L, ctx());

        ArgumentCaptor<MediaAccessLog> row = ArgumentCaptor.forClass(MediaAccessLog.class);
        verify(accessLogMapper).insert(row.capture());
        assertThat(row.getValue().getResult()).isEqualTo("GRANTED");
        assertThat(row.getValue().getAccessKind()).isEqualTo("SIGNED_URL_ISSUED");
        assertThat(row.getValue().getTtlSeconds()).isEqualTo(properties.getSignedUrlTtlSeconds());
        assertThat(row.getValue().getMediaAssetId()).isEqualTo(5L);
    }

    @Test
    void issueSignedUrlRejectsPublicAsset() {
        when(storage.find(5L)).thenReturn(Optional.of(asset(5L, MediaAccessClass.PUBLIC)));

        assertThatThrownBy(() -> service.issueSignedUrl(5L, ctx()))
                .isInstanceOf(IllegalArgumentException.class);
        verify(accessLogMapper, never()).insert(any());
        verify(storage, never()).signedUrl(anyLong(), any());
    }

    @Test
    void openProxyStreamAllowsSensitive() {
        when(storage.find(9L)).thenReturn(Optional.of(asset(9L, MediaAccessClass.SENSITIVE)));
        ProxyStream stream = new ProxyStream(new ByteArrayInputStream(new byte[0]), "application/pdf", 0L, "f.pdf");
        when(storage.openStream(9L)).thenReturn(stream);

        assertThat(service.openProxyStream(9L, ctx())).isSameAs(stream);

        ArgumentCaptor<MediaAccessLog> row = ArgumentCaptor.forClass(MediaAccessLog.class);
        verify(accessLogMapper).insert(row.capture());
        assertThat(row.getValue().getAccessKind()).isEqualTo("STREAM_PROXY");
        assertThat(row.getValue().getResult()).isEqualTo("GRANTED");
    }

    @Test
    void denyAndLogWritesDeniedRow() {
        service.denyAndLog(3L, ctx(), "NO_APPLICANT_GRANT");

        ArgumentCaptor<MediaAccessLog> row = ArgumentCaptor.forClass(MediaAccessLog.class);
        verify(accessLogMapper).insert(row.capture());
        assertThat(row.getValue().getResult()).isEqualTo("DENIED");
        assertThat(row.getValue().getDenyReason()).isEqualTo("NO_APPLICANT_GRANT");
        assertThat(row.getValue().getMediaAssetId()).isEqualTo(3L);
    }
}
