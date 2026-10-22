package com.nadoumi.document.mapper;

import com.nadoumi.document.domain.DocumentTemplate;
import com.nadoumi.document.domain.enums.RequirementScope;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface DocumentTemplateMapper {

    int insert(DocumentTemplate template);

    List<DocumentTemplate> findByScope(@Param("scope") RequirementScope scope, @Param("refId") long refId);

    DocumentTemplate findById(@Param("id") long id);

    int delete(@Param("id") long id);
}
