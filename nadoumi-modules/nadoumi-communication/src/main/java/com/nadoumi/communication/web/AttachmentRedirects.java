package com.nadoumi.communication.web;

import com.nadoumi.communication.web.response.AttachmentAccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** One answer shape for both attachment endpoints: JSON for API callers, a 302 to the signed URL for img/download links. */
final class AttachmentRedirects {

    private AttachmentRedirects() {
    }

    static ResponseEntity<AttachmentAccessResponse> respond(AttachmentAccessResponse access, String json,
            HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        boolean wantsJson = "1".equals(json) || (accept != null && accept.contains("application/json"));
        if (wantsJson) {
            return ResponseEntity.ok(access);
        }
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(access.url())).build();
    }
}
