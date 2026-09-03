package com.nadoumi.media.job;

import com.nadoumi.common.media.MediaStorageService;
import com.nadoumi.media.config.MediaProperties;
import com.nadoumi.media.domain.MediaAsset;
import com.nadoumi.media.mapper.MediaAccessLogMapper;
import com.nadoumi.media.mapper.MediaAssetMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reconciles soft-deleted media with the storage provider (spec §I.8). For every
 * {@code nad_media_asset} that has been {@code DELETED} longer than the configured
 * grace period ({@code nadoumi.media.reconcile-grace-days}, default 30):
 *
 * <ul>
 *   <li>if a {@code nad_media_access_log} row still references it — destroy the
 *       Cloudinary object and mark the row {@code PURGED}, keeping it for audit;</li>
 *   <li>otherwise — destroy the Cloudinary object and hard-delete the row.</li>
 * </ul>
 *
 * <p>Invoked by RuoYi Quartz by bean name from a {@code sys_job} row
 * ({@code invoke_target = 'mediaReconciliationJob.run()'}); the bean is wired in
 * {@code MediaAutoConfiguration}. A single failing asset is logged and skipped so
 * one bad object never aborts the sweep. Logs counts only — never a URL or a
 * secret (spec §I.10).</p>
 */
public class MediaReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(MediaReconciliationJob.class);

    private static final String STATUS_PURGED = "PURGED";
    private static final String SYSTEM_ACTOR = "system";

    private final MediaAssetMapper assetMapper;
    private final MediaAccessLogMapper accessLogMapper;
    private final MediaStorageService storage;
    private final MediaProperties properties;

    public MediaReconciliationJob(MediaAssetMapper assetMapper, MediaAccessLogMapper accessLogMapper,
            MediaStorageService storage, MediaProperties properties) {
        this.assetMapper = assetMapper;
        this.accessLogMapper = accessLogMapper;
        this.storage = storage;
        this.properties = properties;
    }

    /** Quartz entry point. */
    public void run() {
        LocalDateTime before = LocalDateTime.now().minusDays(properties.getReconcileGraceDays());
        List<MediaAsset> candidates = assetMapper.findStaleDeleted(before);

        int purged = 0;
        int hardDeleted = 0;
        int failed = 0;
        for (MediaAsset asset : candidates) {
            long id = asset.getId();
            try {
                if (accessLogMapper.countByAsset(id) > 0) {
                    storage.purge(id);
                    assetMapper.updateStatus(id, STATUS_PURGED, null, SYSTEM_ACTOR, null);
                    purged++;
                } else {
                    storage.purge(id);
                    assetMapper.hardDelete(id);
                    hardDeleted++;
                }
            } catch (RuntimeException e) {
                failed++;
                log.warn("media reconciliation skipped asset id={}: {}", id, e.toString());
            }
        }

        log.info("media reconciliation complete: candidates={} purged={} hardDeleted={} failed={}",
                candidates.size(), purged, hardDeleted, failed);
    }
}
