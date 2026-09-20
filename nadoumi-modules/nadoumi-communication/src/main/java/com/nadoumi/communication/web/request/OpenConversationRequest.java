package com.nadoumi.communication.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Opens a new conversation with an initial message. {@code applicantId} is required
 * for the {@code MESSAGE_STAFF} authorization check -- {@code nad_conversation} itself
 * has no applicant column, only the optional {@code applicationId} context.
 */
public record OpenConversationRequest(
        @NotNull Long applicantId,
        Long applicationId,
        @Size(max = 200) String subject,
        @NotBlank @Size(max = 4000) String body) {
}
