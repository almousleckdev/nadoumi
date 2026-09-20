package com.nadoumi.application.service;

import static com.nadoumi.application.service.WorkflowServiceHarness.application;
import static com.nadoumi.application.service.WorkflowServiceHarness.instance;
import static com.nadoumi.application.service.WorkflowServiceHarness.stage;
import static com.nadoumi.application.service.WorkflowServiceHarness.transition;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.application.domain.WfDefinition;
import com.nadoumi.application.domain.enums.WfDefinitionStatus;
import com.nadoumi.application.domain.enums.WfStageType;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Activating a newer definition version never migrates or re-points a running instance (§3.5). */
class WorkflowVersionPinningTest {

    private final WorkflowServiceHarness h = new WorkflowServiceHarness();

    private static WfDefinition def(long id, String code, int version, WfDefinitionStatus status) {
        WfDefinition d = new WfDefinition();
        d.setId(id);
        d.setCode(code);
        d.setVersion(version);
        d.setStatus(status);
        return d;
    }

    @Test
    void activating_v2_does_not_touch_instances_pinned_to_v1() {
        // definition id=1 is V1 (running instances point at it); id=2 is the new V2 being activated.
        when(h.definitionMapper.findById(2L)).thenReturn(def(2L, "X_V1", 2, WfDefinitionStatus.DRAFT));
        when(h.definitionMapper.findStages(2L)).thenReturn(List.of(
                WorkflowServiceHarness.stage(10L, "START", WfStageType.START),
                WorkflowServiceHarness.stage(11L, "END", WfStageType.TERMINAL)));
        when(h.definitionMapper.findTransitions(2L)).thenReturn(List.of(transition("go", 10L, 11L, null, null)));
        when(h.definitionMapper.findAllVersions("X_V1")).thenReturn(List.of(
                def(1L, "X_V1", 1, WfDefinitionStatus.ACTIVE), def(2L, "X_V1", 2, WfDefinitionStatus.DRAFT)));

        h.service.activate(2L);

        // the running v1 instance's own row is never written by activating v2
        verify(h.instanceMapper, never()).updateStage(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.eq(1L));
        // the prior ACTIVE version (id=1, the one instances are pinned to) is retired, not deleted or altered structurally
        verify(h.definitionMapper).updateStatus(1L, "RETIRED");
        verify(h.definitionMapper).updateStatus(2L, "ACTIVE");
    }

    @Test
    void a_running_instance_keeps_resolving_transitions_against_its_pinned_definition_id() {
        h.stubQuietDefaults();
        // this application's nad_wf_instance was created against definition id=1 (V1) and stays there
        // regardless of whatever is ACTIVE now.
        when(h.applicationMapper.findById(1L)).thenReturn(application(1L, 9L, 20L, 0));
        when(h.instanceMapper.findByApplicationId(1L)).thenReturn(instance(1L, 1L, 20L));
        when(h.definitionMapper.findTransition(1L, "go", 20L)).thenReturn(transition("go", 20L, 21L, null, null));
        when(h.definitionMapper.findStageById(21L)).thenReturn(stage(21L, "NEXT", WfStageType.TERMINAL));
        when(h.definitionMapper.findTransitionsFrom(1L, 21L)).thenReturn(List.of());

        h.service.execute(1L, "go", 50L, false, null, 0);

        verify(h.definitionMapper).findTransition(1L, "go", 20L);
        verify(h.definitionMapper, never()).findTransition(2L, "go", 20L);
    }
}
