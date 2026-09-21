package com.nadoumi.application.service;

import static com.nadoumi.application.service.WorkflowServiceHarness.application;
import static com.nadoumi.application.service.WorkflowServiceHarness.instance;
import static com.nadoumi.application.service.WorkflowServiceHarness.stage;
import static com.nadoumi.application.service.WorkflowServiceHarness.transition;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.nadoumi.application.domain.enums.WfStageType;
import com.nadoumi.application.exception.OptimisticLockException;
import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.exception.NadForbiddenException;
import com.ruoyi.common.utils.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/** Guard enforcement: blocked while unmet, wrong role -> 403, concurrent conflict -> 409. */
class WorkflowGuardTest {

    private final WorkflowServiceHarness h = new WorkflowServiceHarness();

    @Test
    void docs_complete_is_blocked_while_the_DOCUMENTS_COMPLETE_decision_is_missing() {
        h.stubQuietDefaults();
        var app = application(1L, 9L, 20L, 0);
        var inst = instance(1L, 7L, 20L);
        when(h.applicationMapper.findById(1L)).thenReturn(app);
        when(h.instanceMapper.findByApplicationId(1L)).thenReturn(inst);
        when(h.definitionMapper.findTransition(7L, "docs_complete", 20L)).thenReturn(
                transition("docs_complete", 20L, 21L,
                        "{\"all\":[\"DECISION_RECORDED:DOCUMENTS_COMPLETE:CONFIRMED\"]}", "case_officer"));
        when(h.decisionMapper.findByApplicationAndType(1L, "DOCUMENTS_COMPLETE")).thenReturn(java.util.List.of());

        assertThatThrownBy(() -> h.service.execute(1L, "docs_complete", 50L, false, null, 0))
                .isInstanceOf(NadBadRequestException.class)
                .hasMessageContaining("guard not satisfied");
    }

    @Test
    void docs_complete_succeeds_once_the_decision_is_recorded() {
        h.stubQuietDefaults();
        var app = application(1L, 9L, 20L, 0);
        var inst = instance(1L, 7L, 20L);
        when(h.applicationMapper.findById(1L)).thenReturn(app);
        when(h.instanceMapper.findByApplicationId(1L)).thenReturn(inst);
        var t = transition("docs_complete", 20L, 21L,
                "{\"all\":[\"DECISION_RECORDED:DOCUMENTS_COMPLETE:CONFIRMED\"]}", "case_officer");
        when(h.definitionMapper.findTransition(7L, "docs_complete", 20L)).thenReturn(t);
        when(h.definitionMapper.findStageById(21L)).thenReturn(stage(21L, "PACKAGE_PREPARATION", WfStageType.NORMAL));
        when(h.definitionMapper.findTransitionsFrom(7L, 21L)).thenReturn(java.util.List.of());
        var decision = new com.nadoumi.application.domain.ApplicationDecision();
        decision.setOutcome("CONFIRMED");
        when(h.decisionMapper.findByApplicationAndType(1L, "DOCUMENTS_COMPLETE")).thenReturn(java.util.List.of(decision));

        try (MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
            sec.when(() -> SecurityUtils.hasRole("case_officer")).thenReturn(true);
            var result = h.service.execute(1L, "docs_complete", 50L, true, null, 0);
            assertThat(result.getCurrentStageId()).isEqualTo(21L);
        }
    }

    @Test
    void a_staff_transition_requiring_a_role_the_caller_lacks_is_rejected_with_403_not_500() {
        h.stubQuietDefaults();
        var app = application(1L, 9L, 20L, 0);
        var inst = instance(1L, 7L, 20L);
        when(h.applicationMapper.findById(1L)).thenReturn(app);
        when(h.instanceMapper.findByApplicationId(1L)).thenReturn(inst);
        when(h.definitionMapper.findTransition(7L, "claim", 20L))
                .thenReturn(transition("claim", 20L, 21L, null, "case_officer"));

        try (MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
            sec.when(() -> SecurityUtils.hasRole("case_officer")).thenReturn(false);
            assertThatThrownBy(() -> h.service.execute(1L, "claim", 50L, true, null, 0))
                    .isInstanceOf(NadForbiddenException.class);
        }
    }

    @Test
    void a_concurrent_conflicting_transition_yields_409_not_500() {
        h.stubQuietDefaults();
        var app = application(1L, 9L, 20L, 3);
        var inst = instance(1L, 7L, 20L);
        when(h.applicationMapper.findById(1L)).thenReturn(app);
        when(h.instanceMapper.findByApplicationId(1L)).thenReturn(inst);
        when(h.definitionMapper.findTransition(7L, "claim", 20L))
                .thenReturn(transition("claim", 20L, 21L, null, null));
        when(h.definitionMapper.findStageById(21L)).thenReturn(stage(21L, "ELIGIBILITY_REVIEW", WfStageType.NORMAL));
        when(h.applicationMapper.updateWithLock(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(1)))
                .thenReturn(0); // caller believes version is 1, but it has since moved on

        assertThatThrownBy(() -> h.service.execute(1L, "claim", 50L, false, null, 1))
                .isInstanceOf(OptimisticLockException.class);
    }
}
