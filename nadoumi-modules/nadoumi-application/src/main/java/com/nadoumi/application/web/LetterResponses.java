package com.nadoumi.application.web;

import com.nadoumi.common.media.MediaAccessLogContext;
import com.nadoumi.common.media.ProxyStream;
import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.application.domain.enums.LetterKind;
import com.ruoyi.common.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Shared response building for the staff and student letter downloads. */
final class LetterResponses {

    private LetterResponses() {
    }

    static LetterKind kind(String raw) {
        try {
            return LetterKind.valueOf(raw.toUpperCase(java.util.Locale.ROOT));
        }
        catch (IllegalArgumentException e) {
            throw new NadBadRequestException("unknown document kind");
        }
    }

    static MediaAccessLogContext context(Long applicationId, HttpServletRequest request) {
        return new MediaAccessLogContext(SecurityUtils.getUserId(), null, applicationId, null,
                request.getRemoteAddr(), request.getHeader("User-Agent"));
    }

    static ResponseEntity<InputStreamResource> stream(ProxyStream stream, String fallbackName) {
        String name = stream.downloadFilename() == null ? fallbackName : stream.downloadFilename();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(stream.contentType()))
                .contentLength(stream.contentLength())
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(name).build().toString())
                .body(new InputStreamResource(stream.body()));
    }
}
