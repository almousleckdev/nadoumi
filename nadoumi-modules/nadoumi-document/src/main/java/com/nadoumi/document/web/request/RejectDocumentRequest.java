package com.nadoumi.document.web.request;

import jakarta.validation.constraints.NotBlank;

public record RejectDocumentRequest(@NotBlank String reason) {
}
