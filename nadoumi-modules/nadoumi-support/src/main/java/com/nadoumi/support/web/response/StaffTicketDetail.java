package com.nadoumi.support.web.response;

import com.nadoumi.communication.web.response.MessageResponse;
import java.util.List;

public record StaffTicketDetail(
        StaffTicketSummary ticket,
        long conversationId,
        List<MessageResponse> messages,
        List<TicketEventResponse> events) {
}
