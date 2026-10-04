package com.nadoumi.common.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MediaGatewayTest {

    private static final MediaOwnerRef OWNER = new MediaOwnerRef(MediaOwnerKind.ARTICLE, 7L);

    private static final class RecordingGateway implements MediaGateway {
        String filename;
        MediaCategory category;
        InputStream source;

        @Override
        public MediaUploadResult upload(InputStream source, String originalFilename, String declaredContentType,
                long byteSize, MediaCategory category, MediaAccessClass accessClassOrNull, MediaOwnerRef owner,
                long uploadedBy) {
            this.source = source;
            this.filename = originalFilename;
            this.category = category;
            return new MediaUploadResult(11L, "https://cdn/x.png");
        }

        @Override public Optional<StoredAsset> find(long assetId) { return Optional.empty(); }
        @Override public String publicUrl(long assetId) { return null; }
        @Override public SignedUrl issueSignedUrl(long assetId, MediaAccessLogContext ctx) { return null; }
        @Override public SignedUrl issueInlineSignedUrl(long assetId, MediaAccessLogContext ctx) { return null; }
        @Override public ProxyStream openProxyStream(long assetId, MediaAccessLogContext ctx) { return null; }
        @Override public void denyAndLog(long assetId, MediaAccessLogContext ctx, String reason) { }
        @Override public void softDelete(long assetId, long actorUserId) { }
    }

    @Test
    void shouldOpenTheSourceAndDelegate_whenUploadingFromASource() {
        RecordingGateway gateway = new RecordingGateway();

        MediaUploadResult result = gateway.upload(() -> new ByteArrayInputStream(new byte[] {1}), "a.png",
                "image/png", 1, MediaCategory.ARTICLE_COVER, null, OWNER, 3L);

        assertThat(result.mediaId()).isEqualTo(11L);
        assertThat(gateway.filename).isEqualTo("a.png");
        assertThat(gateway.category).isEqualTo(MediaCategory.ARTICLE_COVER);
        assertThat(gateway.source).isNotNull();
    }

    @Test
    void shouldWrapTheIoFailure_whenTheSourceCannotBeOpened() {
        RecordingGateway gateway = new RecordingGateway();

        assertThatThrownBy(() -> gateway.upload(() -> { throw new IOException("disk"); }, "a.png", "image/png", 1,
                MediaCategory.ARTICLE_COVER, null, OWNER, 3L))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("failed to read upload");
    }
}
