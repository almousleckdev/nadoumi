package com.nadoumi.content.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code parentId} null posts a top-level comment; otherwise it is a reply to that comment. */
public record CommentRequest(Long parentId, @NotBlank @Size(max = 2000) String body) {
}
