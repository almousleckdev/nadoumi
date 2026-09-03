package com.nadoumi.media.mapper;

import com.nadoumi.media.domain.MediaAsset;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper for {@code nad_media_asset}. Picked up by the app-wide
 * {@code @MapperScan("com.nadoumi.**.mapper")}, so no {@code @Mapper} annotation.
 */
public interface MediaAssetMapper {

    /** Persist a new asset row; sets the generated {@code id} back on {@code asset}. */
    void insert(MediaAsset asset);

    MediaAsset findById(@Param("id") long id);

    /**
     * Move an asset to a new lifecycle {@code status} (ACTIVE | SUPERSEDED | DELETED),
     * stamping {@code update_by} (RuoYi username) / {@code update_time}. When
     * {@code supersededBy} is given it is written to {@code superseded_by}; when the
     * new status is DELETED, {@code deleted_at = now()} and {@code deleted_by}
     * (a {@code sys_user.user_id}) are also stamped.
     */
    int updateStatus(@Param("id") long id, @Param("status") String status,
            @Param("supersededBy") Long supersededBy, @Param("updateBy") String updateBy,
            @Param("deletedBy") Long deletedBy);

    /** DELETED rows whose {@code deleted_at} is older than {@code before} — purge candidates. */
    List<MediaAsset> findStaleDeleted(@Param("before") LocalDateTime before);

    /**
     * Permanently remove a row once its provider object has been destroyed and
     * nothing references it (spec §I.8). Referenced rows are kept as {@code PURGED}
     * via {@link #updateStatus} instead.
     */
    int hardDelete(@Param("id") long id);

    List<MediaAsset> findByOwner(@Param("kind") String ownerKind, @Param("id") long ownerId);
}
