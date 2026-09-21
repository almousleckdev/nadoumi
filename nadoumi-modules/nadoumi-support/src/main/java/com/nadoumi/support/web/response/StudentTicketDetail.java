package com.nadoumi.support.web.response;

import com.nadoumi.communication.web.response.MessageResponse;
import java.util.List;

/** {@code conversationId} lets the client reuse the conversation endpoints for attachments and read status. */
public record StudentTicketDetail(StudentTicketSummary ticket, long conversationId, List<MessageResponse> messages) {
}
