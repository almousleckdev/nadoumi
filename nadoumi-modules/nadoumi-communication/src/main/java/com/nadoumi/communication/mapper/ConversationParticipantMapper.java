package com.nadoumi.communication.mapper;

import com.nadoumi.communication.domain.ConversationParticipant;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ConversationParticipantMapper {

    /** The caller's own active (not removed) participant row for a conversation, or null. */
    ConversationParticipant findActive(@Param("conversationId") long conversationId, @Param("userId") long userId);

    /** Every active participant row for the given user, across all their conversations. */
    List<ConversationParticipant> listActiveForUser(@Param("userId") long userId);

    List<ConversationParticipant> listActiveForConversation(@Param("conversationId") long conversationId);

    /** True when at least one STAFF-role participant is active on the conversation (the "claimed" signal). */
    boolean hasActiveStaffParticipant(@Param("conversationId") long conversationId);

    int insert(ConversationParticipant participant);

    int remove(@Param("conversationId") long conversationId, @Param("userId") long userId,
            @Param("removedAt") java.time.LocalDateTime removedAt);

    int updateLastRead(@Param("conversationId") long conversationId, @Param("userId") long userId,
            @Param("messageId") long messageId);
}
