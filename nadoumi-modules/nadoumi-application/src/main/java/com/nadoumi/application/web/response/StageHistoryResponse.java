package com.nadoumi.application.web.response;

import com.nadoumi.application.domain.ApplicationStageHistory;

public record StageHistoryResponse(
        Long id, Long fromStageId, Long toStageId, String transitionCode,
        Long changedBy, String changedAt, String reason) {

    public static StageHistoryResponse of(ApplicationStageHistory h) {
        return new StageHistoryResponse(h.getId(), h.getFromStageId(), h.getToStageId(), h.getTransitionCode(),
                h.getChangedBy(), h.getChangedAt() == null ? null : h.getChangedAt().toString(), h.getReason());
    }
}
