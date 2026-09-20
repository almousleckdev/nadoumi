package com.nadoumi.support.web.request;

import com.nadoumi.support.domain.enums.TicketCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * {@code applicantId} is optional -- a student may need help before having an applicant
 * profile. When present the caller must hold {@code MESSAGE_STAFF} on that applicant.
 */
public record CreateTicketRequest(
        @NotBlank @Size(max = 200) String subject,
        @NotNull TicketCategory category,
        @NotBlank @Size(max = 4000) String body,
        Long applicantId) {
}
