package com.nadoumi.content.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Staff create / update payload. The cover is uploaded separately; the slug is derived and then stable. */
public record ArticleRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 300) String subtitle,
        @NotBlank @Size(max = 200_000) String bodyMd,
        @Pattern(regexp = "en|fr|zh|ar|es") String language) {
}
