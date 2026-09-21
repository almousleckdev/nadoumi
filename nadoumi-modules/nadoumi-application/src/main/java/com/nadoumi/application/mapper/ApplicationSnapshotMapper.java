package com.nadoumi.application.mapper;

import com.nadoumi.application.domain.ApplicationSnapshot;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** Immutable. No update/delete statements exist here by design (DA4). */
public interface ApplicationSnapshotMapper {

    List<ApplicationSnapshot> findByApplication(@Param("applicationId") Long applicationId);

    ApplicationSnapshot findLatest(@Param("applicationId") Long applicationId, @Param("kind") String kind);

    int insert(ApplicationSnapshot row);
}
