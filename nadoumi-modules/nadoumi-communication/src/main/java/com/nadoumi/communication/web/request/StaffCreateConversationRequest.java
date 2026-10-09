package com.nadoumi.communication.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StaffCreateConversationRequest(
        @NotNull Long studentUserId,
        Long applicationId,
        @Size(max = 200) String subject,
        @NotBlank @Size(max = 4000) String body) {
}
