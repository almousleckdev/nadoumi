package com.nadoumi.finance.web.response;

import com.nadoumi.finance.domain.Revenue;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Staff-facing view of a revenue record. Omits the raw actor ids/usernames
 * ({@code recordedBy}, {@code createBy}, {@code updateBy}) that {@link Revenue}
 * carries for audit purposes — callers get the resolved display name instead.
 */
public record RevenueResponse(
        long id,
        String source,
        String title,
        String description,
        BigDecimal amount,
        String currency,
        LocalDate receivedOn,
        String reference,
        String relatedType,
        Long relatedId,
        String recordedByName,
        String notes,
        LocalDateTime createTime,
        LocalDateTime updateTime) {

    public static RevenueResponse from(Revenue r) {
        return new RevenueResponse(
                r.getId(), r.getSource(), r.getTitle(), r.getDescription(), r.getAmount(), r.getCurrency(),
                r.getReceivedOn(), r.getReference(), r.getRelatedType(), r.getRelatedId(),
                r.getRecordedByName(), r.getNotes(), r.getCreateTime(), r.getUpdateTime());
    }
}
