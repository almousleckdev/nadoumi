package com.nadoumi.applicant.web;

import com.nadoumi.common.media.MediaAccessLogContext;
import com.nadoumi.common.media.SignedUrl;
import com.ruoyi.common.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Response shapes shared by every applicant protected-file endpoint (photo, passport
 * scan; student and staff): the upload result, the signed-URL JSON, and the redirect
 * for callers that do not ask for JSON.
 */
final class ProtectedMediaResponses {

    private ProtectedMediaResponses() {
    }

    /** {@code POST} result: the new media id only. PROTECTED bytes carry no URL here. */
    record Uploaded(long mediaId) {
    }

    /** {@code GET ?json=1} result. */
    record Url(String url, String expiresAt) {
    }

    static MediaAccessLogContext accessContext(HttpServletRequest request) {
        return new MediaAccessLogContext(actorUserId(), null, null, null,
                request.getRemoteAddr(), request.getHeader("User-Agent"));
    }

    /** JSON when the caller asks for it, otherwise a 302 to the short-lived signed URL. */
    static ResponseEntity<Url> signed(SignedUrl signed, String json, HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        boolean wantsJson = "1".equals(json) || (accept != null && accept.contains("application/json"));
        if (wantsJson) {
            return ResponseEntity.ok(new Url(signed.url(), signed.expiresAt().toString()));
        }
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(signed.url())).build();
    }

    private static long actorUserId() {
        try {
            Long id = SecurityUtils.getUserId();
            return id == null ? 0L : id;
        }
        catch (RuntimeException e) {
            return 0L;
        }
    }
}
