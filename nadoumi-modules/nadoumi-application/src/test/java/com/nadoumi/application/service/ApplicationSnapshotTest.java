package com.nadoumi.application.service;

import static com.nadoumi.application.service.WorkflowServiceHarness.application;
import static com.nadoumi.application.service.WorkflowServiceHarness.instance;
import static com.nadoumi.application.service.WorkflowServiceHarness.stage;
import static com.nadoumi.application.service.WorkflowServiceHarness.transition;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.application.domain.ApplicationSnapshot;
import com.nadoumi.application.domain.enums.WfStageType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Submit writes PROFILE + REQUIREMENTS snapshots exactly once (DA4); later edits never touch them. */
class ApplicationSnapshotTest {

    private final WorkflowServiceHarness h = new WorkflowServiceHarness();

    @Test
    void submit_writes_exactly_two_snapshots_PROFILE_and_REQUIREMENTS() {
        h.stubQuietDefaults();
        when(h.applicationMapper.findById(1L)).thenReturn(application(1L, 9L, 20L, 0));
        when(h.instanceMapper.findByApplicationId(1L)).thenReturn(instance(1L, 7L, 20L));
        when(h.definitionMapper.findTransition(7L, "submit", 20L))
                .thenReturn(transition("submit", 20L, 21L, null, null));
        when(h.definitionMapper.findStageById(21L)).thenReturn(stage(21L, "SUBMITTED", WfStageType.NORMAL));
        when(h.definitionMapper.findTransitionsFrom(7L, 21L)).thenReturn(List.of());
        when(h.applicantService.get(9L)).thenReturn(null);

        h.service.execute(1L, "submit", 50L, false, null, 0);

        ArgumentCaptor<ApplicationSnapshot> captor = ArgumentCaptor.forClass(ApplicationSnapshot.class);
        verify(h.snapshotMapper, times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).extracting(ApplicationSnapshot::getKind)
                .containsExactlyInAnyOrder("PROFILE", "REQUIREMENTS");
        assertThat(captor.getAllValues()).allSatisfy(s -> assertThat(s.getApplicationId()).isEqualTo(1L));
    }

    @Test
    void a_non_submit_transition_writes_no_snapshot_at_all() {
        h.stubQuietDefaults();
        when(h.applicationMapper.findById(1L)).thenReturn(application(1L, 9L, 20L, 0));
        when(h.instanceMapper.findByApplicationId(1L)).thenReturn(instance(1L, 7L, 20L));
        when(h.definitionMapper.findTransition(7L, "claim", 20L))
                .thenReturn(transition("claim", 20L, 21L, null, null));
        when(h.definitionMapper.findStageById(21L)).thenReturn(stage(21L, "ELIGIBILITY_REVIEW", WfStageType.NORMAL));
        when(h.definitionMapper.findTransitionsFrom(7L, 21L)).thenReturn(List.of());

        h.service.execute(1L, "claim", 50L, false, null, 0);

        verify(h.snapshotMapper, org.mockito.Mockito.never()).insert(any());
    }
}
