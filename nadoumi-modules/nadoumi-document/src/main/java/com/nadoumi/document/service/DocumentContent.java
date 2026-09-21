package com.nadoumi.document.service;

import com.nadoumi.common.media.ProxyStream;
import com.nadoumi.common.media.SignedUrl;

/** How a document version's bytes are delivered, resolved from the underlying asset's access class. */
public sealed interface DocumentContent {

    /** PROTECTED — a short-TTL signed URL (redirect or JSON, caller's choice). */
    record Redirect(SignedUrl url) implements DocumentContent {
    }

    /** SENSITIVE — the backend must proxy the bytes; the client never sees a URL. */
    record Proxy(ProxyStream stream) implements DocumentContent {
    }
}
