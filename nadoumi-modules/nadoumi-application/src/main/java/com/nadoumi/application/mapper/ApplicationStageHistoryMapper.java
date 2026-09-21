package com.nadoumi.application.mapper;

import com.nadoumi.application.domain.ApplicationStageHistory;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** Append-only. No update/delete statements exist here by design (history integrity). */
public interface ApplicationStageHistoryMapper {

    List<ApplicationStageHistory> findByApplication(@Param("applicationId") Long applicationId);

    int insert(ApplicationStageHistory row);
}
