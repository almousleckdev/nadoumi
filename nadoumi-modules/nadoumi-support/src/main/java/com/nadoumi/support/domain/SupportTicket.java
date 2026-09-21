package com.nadoumi.support.domain;

import com.nadoumi.support.domain.enums.TicketCategory;
import com.nadoumi.support.domain.enums.TicketPriority;
import com.nadoumi.support.domain.enums.TicketStatus;
import java.time.LocalDateTime;

/** Row of {@code nad_support_ticket}. Owns exactly one SUPPORT conversation. */
public class SupportTicket {

    private Long id;
    private Long conversationId;
    private Long applicantId;
    private Long openedByUserId;
    private String subject;
    private TicketCategory category;
    private TicketPriority priority;
    private TicketStatus status;
    private Long assignedStaffId;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
    private String createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

    public Long getApplicantId() { return applicantId; }
    public void setApplicantId(Long applicantId) { this.applicantId = applicantId; }

    public Long getOpenedByUserId() { return openedByUserId; }
    public void setOpenedByUserId(Long openedByUserId) { this.openedByUserId = openedByUserId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public TicketCategory getCategory() { return category; }
    public void setCategory(TicketCategory category) { this.category = category; }

    public TicketPriority getPriority() { return priority; }
    public void setPriority(TicketPriority priority) { this.priority = priority; }

    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }

    public Long getAssignedStaffId() { return assignedStaffId; }
    public void setAssignedStaffId(Long assignedStaffId) { this.assignedStaffId = assignedStaffId; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
