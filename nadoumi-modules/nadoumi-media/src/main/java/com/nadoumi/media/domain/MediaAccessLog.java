package com.nadoumi.media.domain;

import java.time.LocalDateTime;

/**
 * Row of {@code nad_media_access_log} — one append-only record per PROTECTED /
 * SENSITIVE access attempt (GRANTED or DENIED). Rows are never updated or deleted.
 * Carries its own {@code created_at} column rather than the RuoYi audit quartet.
 */
public class MediaAccessLog {

    private Long id;
    private Long mediaAssetId;
    private Long documentId;
    private Long applicationId;
    private Long actorUserId;
    private Long actorApplicantId;
    private String accessKind;
    private String result;
    private String denyReason;
    private Integer ttlSeconds;
    private String ip;
    private String userAgent;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMediaAssetId() { return mediaAssetId; }
    public void setMediaAssetId(Long mediaAssetId) { this.mediaAssetId = mediaAssetId; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public Long getActorUserId() { return actorUserId; }
    public void setActorUserId(Long actorUserId) { this.actorUserId = actorUserId; }

    public Long getActorApplicantId() { return actorApplicantId; }
    public void setActorApplicantId(Long actorApplicantId) { this.actorApplicantId = actorApplicantId; }

    public String getAccessKind() { return accessKind; }
    public void setAccessKind(String accessKind) { this.accessKind = accessKind; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getDenyReason() { return denyReason; }
    public void setDenyReason(String denyReason) { this.denyReason = denyReason; }

    public Integer getTtlSeconds() { return ttlSeconds; }
    public void setTtlSeconds(Integer ttlSeconds) { this.ttlSeconds = ttlSeconds; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
