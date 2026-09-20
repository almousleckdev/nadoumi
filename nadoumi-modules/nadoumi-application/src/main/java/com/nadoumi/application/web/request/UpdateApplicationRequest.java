package com.nadoumi.application.web.request;

/** Opportunity / intake edits while the application is still DRAFT. */
public record UpdateApplicationRequest(Long programId, Long scholarshipId, Long intakeId) {
}
