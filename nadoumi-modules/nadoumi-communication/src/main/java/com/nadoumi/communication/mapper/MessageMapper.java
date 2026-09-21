package com.nadoumi.communication.mapper;

import com.nadoumi.communication.domain.Message;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface MessageMapper {

    Message findById(@Param("id") long id);

    /** The most recent (non-deleted) message in a conversation, or null when there are none yet. */
    Message findLatest(@Param("conversationId") long conversationId);

    /** Newest-first page, cursor by id (exclusive) -- pass 0 for the first page. */
    List<Message> listByConversation(@Param("conversationId") long conversationId,
            @Param("beforeId") long beforeId, @Param("limit") int limit);

    long countAfter(@Param("conversationId") long conversationId, @Param("afterId") long afterId);

    int insert(Message message);

    int softDelete(@Param("id") long id, @Param("deletedAt") LocalDateTime deletedAt);
}
