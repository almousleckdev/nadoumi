package com.nadoumi.document.mapper;

import com.nadoumi.document.domain.DocumentRequirement;
import com.nadoumi.document.domain.enums.RequirementScope;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface DocumentRequirementMapper {

    List<DocumentRequirement> findByScope(@Param("scope") RequirementScope scope, @Param("refId") long refId);

    /** Checklist for several scope refs at once (e.g. a program + its scholarship + a workflow stage). */
    List<DocumentRequirement> findByScopeRefs(@Param("scope") RequirementScope scope,
            @Param("refIds") Collection<Long> refIds);

    int insert(DocumentRequirement requirement);
}
