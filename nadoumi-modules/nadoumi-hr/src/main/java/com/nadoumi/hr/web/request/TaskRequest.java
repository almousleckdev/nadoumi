package com.nadoumi.hr.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Create/update payload for a task. Status is changed through its own endpoint. */
public record TaskRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 4000) String description,
        @NotBlank String priority,
        Long assigneeUserId,
        LocalDate dueDate,
        String relatedType,
        Long relatedId) {
}
