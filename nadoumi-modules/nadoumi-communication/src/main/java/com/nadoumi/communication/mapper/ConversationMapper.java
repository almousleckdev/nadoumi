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
    List<Conversation> findUnclaimed();

    int insert(Conversation conversation);

    int updateStatus(@Param("id") long id, @Param("status") String status, @Param("updateBy") String updateBy);
}
