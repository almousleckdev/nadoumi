package com.nadoumi.support.mapper;

import com.nadoumi.support.domain.SupportTicket;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** Picked up by the shared {@code @MapperScan("com.nadoumi.**.mapper")}, so no {@code @Mapper}. */
public interface SupportTicketMapper {

    SupportTicket findById(@Param("id") long id);

    int insert(SupportTicket ticket);

    List<SupportTicket> listByOpener(@Param("userId") long userId, @Param("status") String status,
            @Param("offset") int offset, @Param("limit") int limit);

    /** Any filter may be null. */
    List<SupportTicket> listForStaff(@Param("status") String status, @Param("priority") String priority,
            @Param("category") String category, @Param("assigneeId") Long assigneeId,
            @Param("offset") int offset, @Param("limit") int limit);

    /**
     * Compare-and-set on the status: returns 0 when the row is no longer in {@code expected}
     * (a concurrent change), so a stale transition can never be silently applied.
     */
    int updateStatus(@Param("id") long id, @Param("expected") String expected, @Param("status") String status,
            @Param("updateBy") String updateBy);

    int updateAssignee(@Param("id") long id, @Param("assigneeId") Long assigneeId, @Param("updateBy") String updateBy);

    int updatePriority(@Param("id") long id, @Param("priority") String priority, @Param("updateBy") String updateBy);

    int updateCategory(@Param("id") long id, @Param("category") String category, @Param("updateBy") String updateBy);
}
