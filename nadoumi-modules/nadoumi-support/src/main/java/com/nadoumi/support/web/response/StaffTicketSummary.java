package com.nadoumi.support.web.response;

import com.nadoumi.support.domain.SupportTicket;
import java.time.LocalDateTime;

public record StaffTicketSummary(
        long id,
        String subject,
        String category,
        String priority,
        String status,
        long openedByUserId,
        Long applicantId,
        Long assignedStaffId,
        LocalDateTime createTime,
        LocalDateTime updateTime) {

    public static StaffTicketSummary from(SupportTicket t) {
        return new StaffTicketSummary(t.getId(), t.getSubject(), t.getCategory().name(), t.getPriority().name(),
                t.getStatus().name(), t.getOpenedByUserId(), t.getApplicantId(), t.getAssignedStaffId(),
                t.getCreateTime(), t.getUpdateTime());
    }
}
