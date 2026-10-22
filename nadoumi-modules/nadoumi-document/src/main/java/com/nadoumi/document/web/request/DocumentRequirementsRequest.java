package com.nadoumi.document.web.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** The complete list of documents a programme asks for; it replaces what was there. */
public record DocumentRequirementsRequest(
        @NotNull @Size(max = 30) @Valid
        List<Item> requirements) {

    public record Item(
            @NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{1,39}")
            String docType,
            boolean mandatory,
            @Size(max = 255)
            String notes,
            @Pattern(regexp = "MINOR|IN_CHINA|STUDIED_IN_CHINA|MASTER_OR_DOCTORAL|DOCTORAL")
            String appliesWhen) {
    }
}
