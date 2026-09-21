package com.nadoumi.application.mapper;

import com.nadoumi.application.domain.WfDefinition;
import com.nadoumi.application.domain.WfStage;
import com.nadoumi.application.domain.WfStageTaskTemplate;
import com.nadoumi.application.domain.WfTransition;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** Reads/writes the definition layer: {@code nad_wf_definition/stage/transition/stage_task_template}. */
public interface WfDefinitionMapper {

    WfDefinition findById(Long id);

    /** The ACTIVE version of a definition code, or {@code null} if none is active. */
    WfDefinition findActiveByCode(@Param("code") String code);

    List<WfDefinition> findAllVersions(@Param("code") String code);

    List<WfDefinition> list();

    int insert(WfDefinition definition);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    List<WfStage> findStages(@Param("definitionId") Long definitionId);

    WfStage findStageById(Long id);

    List<WfTransition> findTransitions(@Param("definitionId") Long definitionId);

    /** All transitions leaving {@code fromStageId} for this definition, any code. */
    List<WfTransition> findTransitionsFrom(@Param("definitionId") Long definitionId, @Param("fromStageId") Long fromStageId);

    /** The transition family {@code code} leaving {@code fromStageId}, or {@code null}. */
    WfTransition findTransition(@Param("definitionId") Long definitionId, @Param("code") String code,
            @Param("fromStageId") Long fromStageId);

    List<WfStageTaskTemplate> findTaskTemplates(@Param("stageId") Long stageId);
}
