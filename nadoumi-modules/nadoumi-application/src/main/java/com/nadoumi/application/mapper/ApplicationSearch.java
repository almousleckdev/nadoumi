package com.nadoumi.application.mapper;

/**
 * Staff list filter. {@code scopeUserId} set (non-null) restricts to applications
 * assigned to that user or unclaimed (queue) — the {@code case_officer} /
 * {@code document_reviewer} scope from {@code PERMISSION_CATALOGUE.md} §4;
 * {@code null} means org-wide ({@code ops_manager} / super admin).
 */
public record ApplicationSearch(
        String q,
        String applicationType,
        String currentStatus,
        Long stageId,
        Long assigneeUserId,
        Long scopeUserId) {
}
