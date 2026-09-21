package com.nadoumi.application.mapper;

import com.nadoumi.application.domain.WfInstance;
import org.apache.ibatis.annotations.Param;

public interface WfInstanceMapper {

    WfInstance findByApplicationId(@Param("applicationId") Long applicationId);

    int insert(WfInstance instance);

    int updateStage(@Param("applicationId") Long applicationId, @Param("stageId") Long stageId);

    int close(@Param("applicationId") Long applicationId);
}
