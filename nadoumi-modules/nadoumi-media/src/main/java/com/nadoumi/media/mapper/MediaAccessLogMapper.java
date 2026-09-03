package com.nadoumi.media.mapper;

import com.nadoumi.media.domain.MediaAccessLog;

/**
 * MyBatis mapper for {@code nad_media_access_log}. Append-only: {@code insert} is the
 * only operation — rows are never updated or deleted. Picked up by the app-wide
 * {@code @MapperScan("com.nadoumi.**.mapper")}, so no {@code @Mapper} annotation.
 */
public interface MediaAccessLogMapper {

    /** Append one access-attempt record; sets the generated {@code id} back on {@code row}. */
    void insert(MediaAccessLog row);
}
