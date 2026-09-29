package com.nadoumi.common.media;

public final class MediaUrls {

    private static final System.Logger LOG = System.getLogger(MediaUrls.class.getName());

    private MediaUrls() {
    }

    public static String resolve(MediaGateway media, Long mediaId, String legacyUrl) {
        if (mediaId == null) {
            return legacyUrl;
        }
        try {
            return media.publicUrl(mediaId);
        }
        catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "media {0} has no public url, using legacy url", mediaId, e);
            return legacyUrl;
        }
    }
}
