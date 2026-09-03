package com.nadoumi.common.media;

/**
 * Delivery confidentiality tier of a stored media asset (Part I, D5).
 *
 * <ul>
 *   <li>{@code PUBLIC} — served by a stable CDN URL, no authorization.</li>
 *   <li>{@code PROTECTED} — served by a short-TTL signed URL after domain authorization.</li>
 *   <li>{@code SENSITIVE} — never handed to the browser directly; the backend proxies the bytes.</li>
 * </ul>
 */
public enum MediaAccessClass {
    PUBLIC,
    PROTECTED,
    SENSITIVE
}
