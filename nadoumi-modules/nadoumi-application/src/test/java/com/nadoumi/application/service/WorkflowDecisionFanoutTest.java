package com.nadoumi.application.service;

import static com.nadoumi.application.service.WorkflowServiceHarness.application;
import static com.nadoumi.application.service.WorkflowServiceHarness.instance;
import static com.nadoumi.application.service.WorkflowServiceHarness.stage;
import static com.nadoumi.application.service.WorkflowServiceHarness.transition;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.nadoumi.application.domain.ApplicationDecision;
import com.nadoumi.application.domain.enums.WfStageType;
import java.util.List;
import org.junit.jupiter.api.Test;

/** A DECISION stage's outgoing transition is chosen by the recorded decision's outcome, never guessed. */
class WorkflowDecisionFanoutTest {

    private final WorkflowServiceHarness h = new WorkflowServiceHarness();

    private void commonStubs(long fromStageId) {
        h.stubQuietDefaults();
        when(h.applicationMapper.findById(1L)).thenReturn(application(1L, 9L, fromStageId, 0));
        when(h.instanceMapper.findByApplicationId(1L)).thenReturn(instance(1L, 7L, fromStageId));
        when(h.definitionMapper.findTransitionsFrom(7L, 30L)).thenReturn(List.of());
        when(h.definitionMapper.findTransitionsFrom(7L, 31L)).thenReturn(List.of());
    }

    private static ApplicationDecision decision(String outcome) {
        ApplicationDecision d = new ApplicationDecision();
        d.setOutcome(outcome);
        return d;
    }

    @Test
    void uni_offer_fires_only_when_the_recorded_outcome_is_OFFER() {
        commonStubs(20L);
        when(h.definitionMapper.findTransition(7L, "uni_offer", 20L)).thenReturn(
                transition("uni_offer", 20L, 30L, "{\"all\":[\"DECISION_RECORDED:UNIVERSITY_OFFER:OFFER\"]}", null));
        when(h.definitionMapper.findStageById(30L)).thenReturn(stage(30L, "SCHOLARSHIP_DECISION", WfStageType.DECISION));
        when(h.decisionMapper.findByApplicationAndType(1L, "UNIVERSITY_OFFER")).thenReturn(List.of(decision("OFFER")));

        var result = h.service.execute(1L, "uni_offer", 50L, false, null, 0);

        assertThat(result.getCurrentStageId()).isEqualTo(30L);
    }

    @Test
    void uni_reject_fires_only_when_the_recorded_outcome_is_REJECT() {
        commonStubs(20L);
        when(h.definitionMapper.findTransition(7L, "uni_reject", 20L)).thenReturn(
                transition("uni_reject", 20L, 31L, "{\"all\":[\"DECISION_RECORDED:UNIVERSITY_OFFER:REJECT\"]}", null));
        when(h.definitionMapper.findStageById(31L)).thenReturn(stage(31L, "UNSUCCESSFUL", WfStageType.TERMINAL));
        when(h.decisionMapper.findByApplicationAndType(1L, "UNIVERSITY_OFFER")).thenReturn(List.of(decision("REJECT")));

        var result = h.service.execute(1L, "uni_reject", 50L, false, null, 0);

        assertThat(result.getCurrentStageId()).isEqualTo(31L);
    }

    @Test
    void uni_reject_is_blocked_when_the_recorded_outcome_was_actually_OFFER() {
        commonStubs(20L);
        when(h.definitionMapper.findTransition(7L, "uni_reject", 20L)).thenReturn(
                transition("uni_reject", 20L, 31L, "{\"all\":[\"DECISION_RECORDED:UNIVERSITY_OFFER:REJECT\"]}", null));
        when(h.decisionMapper.findByApplicationAndType(1L, "UNIVERSITY_OFFER")).thenReturn(List.of(decision("OFFER")));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> h.service.execute(1L, "uni_reject", 50L, false, null, 0))
                .isInstanceOf(com.nadoumi.common.exception.NadBadRequestException.class);
    }
}
