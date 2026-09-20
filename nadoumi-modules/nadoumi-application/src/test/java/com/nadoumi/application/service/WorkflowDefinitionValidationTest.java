package com.nadoumi.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.nadoumi.application.domain.WfDefinition;
import com.nadoumi.application.domain.WfStage;
import com.nadoumi.application.domain.WfTransition;
import com.nadoumi.application.domain.enums.WfDefinitionStatus;
import com.nadoumi.application.domain.enums.WfStageType;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The six §II.4 definition-validity checks enforced by {@code activate()}. */
class WorkflowDefinitionValidationTest {

    private final WorkflowServiceHarness h = new WorkflowServiceHarness();

    private static WfDefinition definition() {
        WfDefinition d = new WfDefinition();
        d.setId(1L);
        d.setCode("TEST_V1");
        d.setVersion(1);
        d.setStatus(WfDefinitionStatus.DRAFT);
        return d;
    }

    private static WfStage stage(long id, String code, WfStageType type) {
        WfStage s = new WfStage();
        s.setId(id);
        s.setCode(code);
        s.setName(code);
        s.setStageType(type);
        s.setStatusLabel(code);
        return s;
    }

    private static WfTransition transition(String code, Long from, long to, String guardJson) {
        WfTransition t = new WfTransition();
        t.setCode(code);
        t.setFromStageId(from);
        t.setToStageId(to);
        t.setGuardJson(guardJson);
        return t;
    }

    @Test
    void activate_accepts_a_well_formed_definition_and_flips_it_to_ACTIVE() {
        when(h.definitionMapper.findById(1L)).thenReturn(definition());
        when(h.definitionMapper.findStages(1L)).thenReturn(List.of(
                stage(10, "START", WfStageType.START), stage(11, "END", WfStageType.TERMINAL)));
        when(h.definitionMapper.findTransitions(1L)).thenReturn(List.of(transition("go", 10L, 11, null)));
        when(h.definitionMapper.findAllVersions("TEST_V1")).thenReturn(List.of(definition()));

        List<String> problems = h.service.activate(1L);

        assertThat(problems).isEmpty();
        org.mockito.Mockito.verify(h.definitionMapper).updateStatus(1L, "ACTIVE");
    }

    @Test
    void activate_rejects_a_definition_with_no_START_stage() {
        when(h.definitionMapper.findById(1L)).thenReturn(definition());
        when(h.definitionMapper.findStages(1L)).thenReturn(List.of(stage(11, "END", WfStageType.TERMINAL)));
        when(h.definitionMapper.findTransitions(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> h.service.activate(1L)).hasMessageContaining("START");
    }

    @Test
    void activate_rejects_a_definition_with_no_TERMINAL_stage() {
        when(h.definitionMapper.findById(1L)).thenReturn(definition());
        when(h.definitionMapper.findStages(1L)).thenReturn(List.of(stage(10, "START", WfStageType.START)));
        when(h.definitionMapper.findTransitions(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> h.service.activate(1L)).hasMessageContaining("TERMINAL");
    }

    @Test
    void activate_rejects_a_non_terminal_stage_with_no_outgoing_transition() {
        when(h.definitionMapper.findById(1L)).thenReturn(definition());
        when(h.definitionMapper.findStages(1L)).thenReturn(List.of(
                stage(10, "START", WfStageType.START), stage(11, "STUCK", WfStageType.NORMAL),
                stage(12, "END", WfStageType.TERMINAL)));
        when(h.definitionMapper.findTransitions(1L)).thenReturn(List.of(transition("go", 10L, 11, null)));

        assertThatThrownBy(() -> h.service.activate(1L)).hasMessageContaining("STUCK");
    }

    @Test
    void activate_rejects_an_unreachable_stage() {
        when(h.definitionMapper.findById(1L)).thenReturn(definition());
        when(h.definitionMapper.findStages(1L)).thenReturn(List.of(
                stage(10, "START", WfStageType.START), stage(11, "END", WfStageType.TERMINAL),
                stage(12, "ORPHAN", WfStageType.TERMINAL)));
        when(h.definitionMapper.findTransitions(1L)).thenReturn(List.of(transition("go", 10L, 11, null)));

        assertThatThrownBy(() -> h.service.activate(1L)).hasMessageContaining("ORPHAN").hasMessageContaining("unreachable");
    }

    @Test
    void activate_rejects_a_DECISION_stage_with_fewer_than_two_DECISION_RECORDED_transitions() {
        when(h.definitionMapper.findById(1L)).thenReturn(definition());
        when(h.definitionMapper.findStages(1L)).thenReturn(List.of(
                stage(10, "START", WfStageType.START), stage(11, "DEC", WfStageType.DECISION),
                stage(12, "END", WfStageType.TERMINAL)));
        when(h.definitionMapper.findTransitions(1L)).thenReturn(List.of(
                transition("toDecision", 10L, 11, null),
                transition("onlyOne", 11L, 12, "{\"all\":[\"DECISION_RECORDED:X:Y\"]}")));

        assertThatThrownBy(() -> h.service.activate(1L)).hasMessageContaining("DEC").hasMessageContaining(">=2");
    }

    @Test
    void activate_rejects_a_definition_referencing_an_unbacked_predicate_no_provider_registered() {
        when(h.definitionMapper.findById(1L)).thenReturn(definition());
        when(h.definitionMapper.findStages(1L)).thenReturn(List.of(
                stage(10, "START", WfStageType.START), stage(11, "END", WfStageType.TERMINAL)));
        when(h.definitionMapper.findTransitions(1L)).thenReturn(List.of(
                transition("go", 10L, 11, "{\"all\":[\"ALL_REQUIRED_DOCUMENTS_ATTACHED\"]}")));

        assertThatThrownBy(() -> h.service.activate(1L))
                .hasMessageContaining("UNBACKED_GUARD_PREDICATE")
                .hasMessageContaining("ALL_REQUIRED_DOCUMENTS_ATTACHED");
    }
}
