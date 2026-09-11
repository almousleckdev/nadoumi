package com.nadoumi.identity.service.mail;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nadoumi brand + contact details rendered into every transactional email
 * ({@link EmailLayout}). Bound from {@code nadoumi.brand.*}; the values must stay
 * in sync with {@code nadoumi-web/app/data/contact.ts} until a shared API exists
 * (see {@code docs/DEVELOPMENT_GUIDELINES.md}).
 *
 * <p>{@code baseUrl} and {@code assetBaseUrl} are deliberately separate origins:
 * the public site (Nuxt, {@code nadoumi-web}) and this API are different
 * deployments. CTA/content links must resolve on the public site; the logo and
 * social icons are files this backend itself serves under {@code /email/*}, so
 * they must resolve on this backend's own origin, not the site's.
 *
 * @param baseUrl         public site origin, no trailing slash — CTA and content links
 * @param assetBaseUrl    this backend's own public origin, no trailing slash — serves
 *                        {@code /email/*} (logo + social icons); falls back to
 *                        {@code baseUrl} when unset, for deployments where both are
 *                        the same origin
 * @param logoPath        absolute path of the footer logo under {@code assetBaseUrl}
 * @param wordmark        plain-text wordmark shown in the header
 * @param contact         support contact block for the footer
 * @param social          social profile URLs; a blank/null one hides that icon
 * @param preferencesPath path of the notification-preferences page (non-transactional footer link)
 */
@ConfigurationProperties("nadoumi.brand")
public record BrandProperties(
        String baseUrl,
        String assetBaseUrl,
        String logoPath,
        String wordmark,
        Contact contact,
        Social social,
        String preferencesPath) {

    public BrandProperties {
        baseUrl = stripTrailingSlash(baseUrl == null || baseUrl.isBlank() ? "http://localhost:3000" : baseUrl.trim());
        assetBaseUrl = assetBaseUrl == null || assetBaseUrl.isBlank() ? baseUrl : stripTrailingSlash(assetBaseUrl.trim());
        logoPath = blankTo(logoPath, "/email/nadoumi-logo.png");
        wordmark = blankTo(wordmark, "Nadoumi");
        preferencesPath = blankTo(preferencesPath, "/account/notifications");
        contact = contact == null ? new Contact(List.of(), List.of(), null, null) : contact;
        social = social == null ? new Social(null, null, null, null) : social;
    }

    /** @param emails support inboxes, rendered as {@code mailto:} links */
    /** @param phones support numbers, rendered as {@code tel:} links */
    public record Contact(List<String> emails, List<String> phones, String officeEn, String hours) {
        public Contact {
            emails = emails == null ? List.of() : List.copyOf(emails);
            phones = phones == null ? List.of() : List.copyOf(phones);
        }
    }

    public record Social(String facebook, String instagram, String tiktok, String whatsapp) {
    }

    public String logoUrl() {
        return assetBaseUrl + logoPath;
    }

    public String preferencesUrl() {
        return baseUrl + preferencesPath;
    }

    /** Absolute URL for a public-site app path such as {@code /scholarships}. */
    public String url(String path) {
        return absolute(baseUrl, path);
    }

    /** Absolute URL for a path this backend itself serves, such as {@code /email/facebook.png}. */
    public String assetUrl(String path) {
        return absolute(assetBaseUrl, path);
    }

    private static String absolute(String origin, String path) {
        if (path == null || path.isBlank()) {
            return origin;
        }
        return path.startsWith("http") ? path : origin + (path.startsWith("/") ? path : "/" + path);
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
