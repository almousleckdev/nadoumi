package com.nadoumi.communication.web.response;

import java.time.LocalDateTime;
import java.util.List;

public record MessageResponse(
        long id,
        long conversationId,
        long senderUserId,
        String senderName,
        String body,
        LocalDateTime createdAt,
        LocalDateTime editedAt,
        List<AttachmentResponse> attachments) {
}
