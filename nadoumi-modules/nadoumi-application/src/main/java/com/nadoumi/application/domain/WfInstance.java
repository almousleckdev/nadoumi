package com.nadoumi.application.domain;

import com.nadoumi.application.domain.enums.WfInstanceStatus;
import java.time.LocalDateTime;

/** Row of {@code nad_wf_instance} — one running (or closed) instance, 1:1 with an {@link Application}. */
public class WfInstance {

    private Long id;
    private Long definitionId;
    private int definitionVersion;
    private Long applicationId;
    private Long currentStageId;
    private WfInstanceStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime closedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDefinitionId() { return definitionId; }
    public void setDefinitionId(Long definitionId) { this.definitionId = definitionId; }

    public int getDefinitionVersion() { return definitionVersion; }
    public void setDefinitionVersion(int definitionVersion) { this.definitionVersion = definitionVersion; }

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public Long getCurrentStageId() { return currentStageId; }
    public void setCurrentStageId(Long currentStageId) { this.currentStageId = currentStageId; }

    public WfInstanceStatus getStatus() { return status; }
    public void setStatus(WfInstanceStatus status) { this.status = status; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
}
