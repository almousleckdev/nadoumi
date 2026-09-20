package com.nadoumi.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.nadoumi.application.domain.WfDefinition;
import com.nadoumi.application.domain.WfStage;
import com.nadoumi.application.domain.enums.ApplicationType;
import com.nadoumi.application.domain.enums.WfDefinitionStatus;
import com.nadoumi.application.domain.enums.WfStageType;
import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.program.web.response.ProgramResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** PROGRAM_ONLY routes to PROGRAM_ONLY_V1, and rejects a non-null scholarshipId (DA6). */
class ProgramOnlyWorkflowTest {

    private final WorkflowServiceHarness h = new WorkflowServiceHarness();

    private static ProgramResponse program(long id) {
        return new ProgramResponse(id, 1L, "Test Uni", "test-uni", "Computer Science", "computer-science",
                null, "MASTER", List.of("MASTER"), null, null, "ENGLISH", 24, null, null, null, null, null, null,
                false, false, "ACTIVE", "PUBLISHED", null, null, null, List.of(), List.of());
    }

    @Test
    void a_PROGRAM_ONLY_application_carrying_a_scholarshipId_is_rejected_before_any_write() {
        assertThatThrownBy(() -> h.service.startDraft(9L, ApplicationType.PROGRAM_ONLY, 1L, 77L, null, 50L))
                .isInstanceOf(NadBadRequestException.class)
                .hasMessageContaining("scholarship_id");
        org.mockito.Mockito.verifyNoInteractions(h.applicationMapper);
    }

    @Test
    void a_PROGRAM_WITH_SCHOLARSHIP_application_with_no_scholarshipId_is_rejected() {
        assertThatThrownBy(() -> h.service.startDraft(9L, ApplicationType.PROGRAM_WITH_SCHOLARSHIP, 1L, null, null, 50L))
                .isInstanceOf(NadBadRequestException.class)
                .hasMessageContaining("scholarship_id");
    }

    @Test
    void a_valid_PROGRAM_ONLY_draft_routes_to_PROGRAM_ONLY_V1_and_starts_at_its_START_stage() {
        when(h.programService.get(1L)).thenReturn(program(1L));
        WfDefinition def = new WfDefinition();
        def.setId(3L);
        def.setCode("PROGRAM_ONLY_V1");
        def.setVersion(1);
        def.setStatus(WfDefinitionStatus.ACTIVE);
        when(h.definitionMapper.findActiveByCode("PROGRAM_ONLY_V1")).thenReturn(def);
        WfStage start = new WfStage();
        start.setId(10L);
        start.setCode("DRAFT");
        start.setStageType(WfStageType.START);
        start.setStatusLabel("DRAFT");
        when(h.definitionMapper.findStages(3L)).thenReturn(List.of(start));
        when(h.definitionMapper.findTaskTemplates(10L)).thenReturn(List.of());
        // simulate MyBatis useGeneratedKeys: insert() sets the id on the very object it was passed
        org.mockito.Mockito.doAnswer(inv -> {
            com.nadoumi.application.domain.Application a = inv.getArgument(0);
            a.setId(1L);
            return 1;
        }).when(h.applicationMapper).insert(org.mockito.ArgumentMatchers.any());
        when(h.applicationMapper.findById(1L)).thenAnswer(inv -> {
            var a = new com.nadoumi.application.domain.Application();
            a.setId(1L);
            a.setCurrentStageId(10L);
            a.setCurrentStatus("DRAFT");
            return a;
        });

        h.service.startDraft(9L, ApplicationType.PROGRAM_ONLY, 1L, null, null, 50L);

        ArgumentCaptor<com.nadoumi.application.domain.Application> captor =
                ArgumentCaptor.forClass(com.nadoumi.application.domain.Application.class);
        org.mockito.Mockito.verify(h.applicationMapper).insert(captor.capture());
        assertThat(captor.getValue().getApplicationType()).isEqualTo(ApplicationType.PROGRAM_ONLY);
        assertThat(captor.getValue().getScholarshipId()).isNull();
        assertThat(captor.getValue().getCurrentStageId()).isEqualTo(10L);
    }

    @Test
    void an_intake_that_does_not_belong_to_the_chosen_programme_is_rejected() {
        when(h.programService.get(1L)).thenReturn(program(1L)); // no intakes at all

        assertThatThrownBy(() -> h.service.startDraft(9L, ApplicationType.PROGRAM_ONLY, 1L, null, 999L, 50L))
                .isInstanceOf(NadBadRequestException.class)
                .hasMessageContaining("intake");
    }
}
