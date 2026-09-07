package com.nadoumi.hr.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;

/** Read-only RBAC lookups for notification fan-out. */
public interface HrAudienceMapper {

    /** Active staff who hold {@code perm} through any assigned role. */
    List<Long> findStaffUserIdsWithPermission(@Param("perm") String perm);
}
