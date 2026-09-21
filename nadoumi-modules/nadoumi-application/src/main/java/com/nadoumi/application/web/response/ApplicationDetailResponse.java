package com.nadoumi.application.web.response;

import java.util.List;

/**
 * Full staff-side view: application + current tasks + history + events + decisions.
 * Staff-only — never returned to a student caller (that is {@link StudentApplicationResponse}).
 */
public record ApplicationDetailResponse(
        ApplicationResponse application,
        List<TaskResponse> tasks,
        List<StageHistoryResponse> history,
        List<ApplicationEventResponse> events,
        List<DecisionResponse> decisions) {
}
