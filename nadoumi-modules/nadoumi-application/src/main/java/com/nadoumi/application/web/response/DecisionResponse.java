package com.nadoumi.application.web.response;

import com.nadoumi.application.domain.ApplicationDecision;

public record DecisionResponse(
        Long id, String decisionType, String outcome, String rationale,
        Long decidedBy, String decidedAt, String decidedByName) {

    public static DecisionResponse of(ApplicationDecision d, String decidedByName) {
        return new DecisionResponse(d.getId(), d.getDecisionType(), d.getOutcome(), d.getRationale(),
                d.getDecidedBy(), d.getDecidedAt() == null ? null : d.getDecidedAt().toString(), decidedByName);
    }
}
