package com.nadoumi.communication.mapper;

import com.nadoumi.communication.domain.Conversation;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ConversationMapper {

    Conversation findById(@Param("id") long id);

    List<Conversation> findByIds(@Param("ids") Collection<Long> ids);

    List<Conversation> findByApplicationId(@Param("applicationId") long applicationId);

    /**
     * OPEN conversations with no active STAFF participant yet -- the discovery
     * queue a staff member with {@code nad:conversation:participate} joins from.
     * Not part of the original endpoint list; a minimal join mechanism without
     * which staff could never discover a student-opened conversation to add
     * themselves to (see this fork's final report).
     */
    List<Conversation> searchForStaff(
            @Param("staffUserId") long staffUserId,
            @Param("studentName") String studentName,
            @Param("applicationId") Long applicationId
    );

    /** The caller's conversations, newest activity first, with unread counts and the other side, in one query. */
    List<InboxRow> listInbox(@Param("userId") long userId, @Param("query") String query,
            @Param("applicationId") Long applicationId, @Param("conversationId") Long conversationId,
            @Param("limit") int limit, @Param("offset") int offset);

    /** Unread messages across all of the user's conversations (the sidebar badge). */
    long countUnread(@Param("userId") long userId);

    Conversation findByDirectKey(@Param("directKey") String directKey);

    int insert(Conversation conversation);

    int updateLastMessage(@Param("id") long id, @Param("messageId") long messageId, @Param("at") java.time.LocalDateTime at,
            @Param("preview") String preview, @Param("senderUserId") long senderUserId);

    /** Removes the conversation; its messages, attachments and participants go with it (ON DELETE CASCADE). */
    int deleteById(@Param("id") long id);

    int updateStatus(@Param("id") long id, @Param("status") String status, @Param("updateBy") String updateBy);
}
