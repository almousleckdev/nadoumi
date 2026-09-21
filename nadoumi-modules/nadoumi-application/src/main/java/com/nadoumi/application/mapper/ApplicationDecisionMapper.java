package com.nadoumi.application.mapper;

import com.nadoumi.application.domain.ApplicationDecision;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** Append-only. No update/delete statements exist here by design (history integrity). */
public interface ApplicationDecisionMapper {

    List<ApplicationDecision> findByApplication(@Param("applicationId") Long applicationId);

    /** Decisions of one type for an application, newest first — used by the guard evaluator. */
    List<ApplicationDecision> findByApplicationAndType(@Param("applicationId") Long applicationId,
            @Param("decisionType") String decisionType);

    int insert(ApplicationDecision row);
}
