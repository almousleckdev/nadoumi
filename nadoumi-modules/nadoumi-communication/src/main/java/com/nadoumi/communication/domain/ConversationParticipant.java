package com.nadoumi.communication.domain;

import com.nadoumi.communication.domain.enums.ParticipantRole;
import java.time.LocalDateTime;

/** Row of {@code nad_conversation_participant}. */
public class ConversationParticipant {

    private Long id;
    private Long conversationId;
    private Long userId;
    private ParticipantRole role;
    private LocalDateTime addedAt;
    private LocalDateTime removedAt;
    private Long lastReadMessageId;
    private boolean muted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public ParticipantRole getRole() { return role; }
    public void setRole(ParticipantRole role) { this.role = role; }

    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }

    public LocalDateTime getRemovedAt() { return removedAt; }
    public void setRemovedAt(LocalDateTime removedAt) { this.removedAt = removedAt; }

    public Long getLastReadMessageId() { return lastReadMessageId; }
    public void setLastReadMessageId(Long lastReadMessageId) { this.lastReadMessageId = lastReadMessageId; }

    public boolean isMuted() { return muted; }
    public void setMuted(boolean muted) { this.muted = muted; }
}
