package com.nadoumi.notification.domain;

import java.time.LocalDateTime;

/**
 * Row of {@code nad_outbox_event} — one domain event awaiting delivery to the
 * notification pipeline. Written in the producer's transaction; drained by
 * {@code OutboxPollerJob}.
 *
 * <p>{@link #payloadJson} carries safe scalars only — never PII or confidential
 * fields ({@code docs/SECURITY.md} §6).</p>
 */
public class OutboxEvent {

    private Long id;
    private String aggregateType;
    private Long aggregateId;
    private String type;
    private String payloadJson;
    private String status;
    private int retryCount;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime nextAttemptAt;
    private LocalDateTime processedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAggregateType() { return aggregateType; }
    public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }

    public Long getAggregateId() { return aggregateId; }
    public void setAggregateId(Long aggregateId) { this.aggregateId = aggregateId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(LocalDateTime nextAttemptAt) { this.nextAttemptAt = nextAttemptAt; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    /** @return the parsed {@link OutboxStatus}, or {@code null} when unset. */
    public OutboxStatus getStatusEnum() {
        return status == null ? null : OutboxStatus.valueOf(status);
    }
}
