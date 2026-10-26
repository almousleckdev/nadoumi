package com.nadoumi.application.mapper;

import com.nadoumi.application.domain.ApplicationInterview;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ApplicationInterviewMapper {

    /** Every interview of an application, newest first (cancelled ones are history, not deleted). */
    List<ApplicationInterview> findByApplication(@Param("applicationId") Long applicationId);

    ApplicationInterview findScheduled(@Param("applicationId") Long applicationId);

    ApplicationInterview findById(@Param("id") Long id);

    int insert(ApplicationInterview row);

    /** Cancels one interview if it is still SCHEDULED; 0 rows when it already was not. */
    int cancel(@Param("id") Long id, @Param("updateBy") String updateBy);
}
