package com.nadoumi.communication.domain;

import com.nadoumi.communication.domain.enums.ConversationStatus;
import com.nadoumi.communication.domain.enums.ConversationType;
import java.time.LocalDateTime;

/** Row of {@code nad_conversation}. One application may have many conversations. */
public class Conversation {

    private Long id;
    private String subject;
    private Long applicationId;
    private ConversationType conversationType;
    private ConversationStatus status;
    private String createBy;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public ConversationType getConversationType() { return conversationType; }
    public void setConversationType(ConversationType conversationType) { this.conversationType = conversationType; }

    public ConversationStatus getStatus() { return status; }
    public void setStatus(ConversationStatus status) { this.status = status; }

    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
