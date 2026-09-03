package com.nadoumi.media.mapper;

import com.nadoumi.media.domain.MediaAccessLog;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper for {@code nad_media_access_log}. Append-only: {@code insert} is the
 * only write — rows are never updated or deleted. Picked up by the app-wide
 * {@code @MapperScan("com.nadoumi.**.mapper")}, so no {@code @Mapper} annotation.
 */
public interface MediaAccessLogMapper {

    /** Append one access-attempt record; sets the generated {@code id} back on {@code row}. */
    void insert(MediaAccessLog row);

    /**
     * How many audit rows reference {@code assetId}. Used by
     * {@code MediaReconciliationJob} to decide whether a soft-deleted asset row
     * must be kept (status {@code PURGED}) or may be hard-deleted.
     */
    int countByAsset(@Param("assetId") long assetId);
}
