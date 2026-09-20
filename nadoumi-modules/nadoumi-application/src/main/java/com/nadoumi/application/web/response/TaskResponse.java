package com.nadoumi.application.web.response;

import com.nadoumi.application.domain.ApplicationTask;

public record TaskResponse(
        Long id, String title, String roleRequired, boolean mandatory, boolean blocksExit,
        String status, Long assigneeUserId, String dueAt, String skipReason) {

    public static TaskResponse of(ApplicationTask t) {
        return new TaskResponse(t.getId(), t.getTitle(), t.getRoleRequired(), t.isMandatory(), t.isBlocksExit(),
                t.getStatus() == null ? null : t.getStatus().name(), t.getAssigneeUserId(),
                t.getDueAt() == null ? null : t.getDueAt().toString(), t.getSkipReason());
    }
}
