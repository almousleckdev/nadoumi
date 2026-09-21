package com.nadoumi.document.web;

import com.nadoumi.common.media.MediaAccessLogContext;
import com.nadoumi.common.media.ProxyStream;
import com.nadoumi.common.media.SignedUrl;
import com.nadoumi.document.service.DocumentContent;
import com.ruoyi.common.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Locale;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Turns a resolved {@link DocumentContent} into an HTTP response: a redirect/JSON
 * for a PROTECTED signed URL, or a bounded-buffer stream proxy for SENSITIVE bytes
 * (DOCUMENT_MANAGEMENT.md §3.2a — never buffered whole in memory).
 */
final class DocumentContentResponses {

    private static final int BUFFER_SIZE = 8192;

    private DocumentContentResponses() {
    }

    static MediaAccessLogContext accessContext(HttpServletRequest request, Long documentId) {
        return new MediaAccessLogContext(actorUserId(), null, null, documentId,
                request.getRemoteAddr(), request.getHeader("User-Agent"));
    }

    /** {@code GET ?json=1} result for a redirect-delivered file. */
    record Url(String url, String expiresAt) {
    }

    static ResponseEntity<?> respond(DocumentContent content, String json, HttpServletRequest request) {
        if (content instanceof DocumentContent.Redirect redirect) {
            return redirect(redirect.url(), json, request);
        }
        if (content instanceof DocumentContent.Proxy proxy) {
            return proxy(proxy.stream());
        }
        throw new IllegalStateException("unknown DocumentContent variant: " + content);
    }

    private static ResponseEntity<?> redirect(SignedUrl signed, String json, HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        boolean wantsJson = "1".equals(json) || (accept != null && accept.contains("application/json"));
        if (wantsJson) {
            return ResponseEntity.ok(new Url(signed.url(), signed.expiresAt().toString()));
        }
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(signed.url())).build();
    }

    private static ResponseEntity<StreamingResponseBody> proxy(ProxyStream stream) {
        StreamingResponseBody body = out -> {
            try (var in = stream.body()) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
        };
        String contentType = stream.contentType() != null ? stream.contentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(stream.contentLength()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(stream.downloadFilename()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(body);
    }

    private static String contentDisposition(String filename) {
        String safe = filename == null ? "download" : filename;
        return String.format(Locale.ROOT, "attachment; filename=\"%s\"", safe.replace("\"", ""));
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
