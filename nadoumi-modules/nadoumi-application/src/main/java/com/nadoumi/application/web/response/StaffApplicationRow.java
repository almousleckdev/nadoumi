package com.nadoumi.application.web.response;

/** One line of the staff application table: the application with the names a case officer reads. */
public record StaffApplicationRow(
        Long id,
        String reference,
        Long applicantId,
        String applicantName,
        String applicationType,
        String opportunityTitle,
        String universityName,
        String intake,
        String currentStageName,
        String currentStatus,
        Long assigneeUserId,
        String assigneeName,
        String submittedAt) {
}
