package com.nadoumi.support.web.response;

import com.nadoumi.support.domain.SupportTicket;
import java.time.LocalDateTime;

/** Student-facing ticket row. Deliberately omits priority, assignee and other internal triage data. */
public record StudentTicketSummary(
        long id,
        String subject,
        String category,
        String status,
        LocalDateTime createTime,
        LocalDateTime updateTime) {

    public static StudentTicketSummary from(SupportTicket t) {
        return new StudentTicketSummary(t.getId(), t.getSubject(), t.getCategory().name(),
                t.getStatus().name(), t.getCreateTime(), t.getUpdateTime());
    }
}
