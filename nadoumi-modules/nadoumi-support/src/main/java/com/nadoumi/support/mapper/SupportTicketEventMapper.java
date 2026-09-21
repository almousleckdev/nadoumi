package com.nadoumi.support.mapper;

import com.nadoumi.support.domain.SupportTicketEvent;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface SupportTicketEventMapper {

    int insert(SupportTicketEvent event);

    /** Oldest first. */
    List<SupportTicketEvent> listByTicket(@Param("ticketId") long ticketId);
}
