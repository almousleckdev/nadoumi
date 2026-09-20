package com.nadoumi.application.mapper;

import com.nadoumi.application.domain.ApplicationTask;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ApplicationTaskMapper {

    ApplicationTask findById(Long id);

    List<ApplicationTask> findByApplication(@Param("applicationId") Long applicationId);

    List<ApplicationTask> findByApplicationAndStatus(@Param("applicationId") Long applicationId,
            @Param("status") String status);

    int insert(ApplicationTask task);

    int updateStatus(@Param("id") Long id, @Param("status") String status,
            @Param("skipReason") String skipReason, @Param("updateBy") String updateBy);
}
