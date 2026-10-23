package com.nadoumi.program.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A catalogue major. {@code discipline} is a free group code such as MEDICINE; blank means ungrouped. */
public record MajorRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 200) String nameCn,
        @Size(max = 32) String discipline) {
}
