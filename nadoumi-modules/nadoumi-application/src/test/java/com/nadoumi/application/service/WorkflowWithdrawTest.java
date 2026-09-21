package com.nadoumi.application.service;

import static com.nadoumi.application.service.WorkflowServiceHarness.application;
import static com.nadoumi.application.service.WorkflowServiceHarness.instance;
import static com.nadoumi.application.service.WorkflowServiceHarness.stage;
import static com.nadoumi.application.service.WorkflowServiceHarness.transition;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.application.domain.enums.WfStageType;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** {@code withdraw} from any non-terminal stage lands in WITHDRAWN, one history row, instance closed. */
class WorkflowWithdrawTest {

    private static Stream<Long> nonTerminalStages() {
        return Stream.of(20L, 21L, 22L, 23L);
    }

    @ParameterizedTest
    @MethodSource("nonTerminalStages")
    void withdraw_from_any_non_terminal_stage_closes_the_application(long fromStageId) {
        WorkflowServiceHarness h = new WorkflowServiceHarness();
        h.stubQuietDefaults();
        when(h.applicationMapper.findById(1L)).thenReturn(application(1L, 9L, fromStageId, 0));
        when(h.instanceMapper.findByApplicationId(1L)).thenReturn(instance(1L, 7L, fromStageId));
        when(h.definitionMapper.findTransition(7L, "withdraw", fromStageId))
                .thenReturn(transition("withdraw", fromStageId, 99L, null, null));
        when(h.definitionMapper.findStageById(99L)).thenReturn(stage(99L, "WITHDRAWN", WfStageType.TERMINAL));

        var result = h.service.execute(1L, "withdraw", 50L, false, "changed my mind", 0);

        assertThat(result.getCurrentStageId()).isEqualTo(99L);
        verify(h.historyMapper, times(1)).insert(any());
        verify(h.instanceMapper).close(1L);
    }

    @Test
    void withdraw_without_a_reason_is_still_accepted_by_the_engine_reason_is_optional_at_this_layer() {
        // The student API requires a reason for `withdraw` (StudentApplicationService); the engine
        // itself does not — staff may withdraw with an empty reason via the admin API.
        WorkflowServiceHarness h = new WorkflowServiceHarness();
        h.stubQuietDefaults();
        when(h.applicationMapper.findById(1L)).thenReturn(application(1L, 9L, 20L, 0));
        when(h.instanceMapper.findByApplicationId(1L)).thenReturn(instance(1L, 7L, 20L));
        when(h.definitionMapper.findTransition(7L, "withdraw", 20L))
                .thenReturn(transition("withdraw", 20L, 99L, null, null));
        when(h.definitionMapper.findStageById(99L)).thenReturn(stage(99L, "WITHDRAWN", WfStageType.TERMINAL));

        var result = h.service.execute(1L, "withdraw", 50L, true, null, 0);

        assertThat(result.getCurrentStageId()).isEqualTo(99L);
    }
}
