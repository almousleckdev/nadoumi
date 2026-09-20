package com.nadoumi.communication.domain;

import java.time.LocalDateTime;

/** Row of {@code nad_message_attachment}. A plain media asset, never auto-promoted to a document. */
public class MessageAttachment {

    private Long id;
    private Long messageId;
    private Long mediaAssetId;
    private Long promotedDocumentId;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }

    public Long getMediaAssetId() { return mediaAssetId; }
    public void setMediaAssetId(Long mediaAssetId) { this.mediaAssetId = mediaAssetId; }

    public Long getPromotedDocumentId() { return promotedDocumentId; }
    public void setPromotedDocumentId(Long promotedDocumentId) { this.promotedDocumentId = promotedDocumentId; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
