package com.nadoumi.identity.service.mail;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nadoumi brand + contact details rendered into every transactional email
 * ({@link EmailLayout}). Bound from {@code nadoumi.brand.*}; the values must stay
 * in sync with {@code nadoumi-web/app/data/contact.ts} until a shared API exists
 * (see {@code docs/DEVELOPMENT_GUIDELINES.md}).
 *
 * @param baseUrl         public site origin, no trailing slash (also hosts the email assets)
 * @param logoPath        absolute path of the footer logo under {@code baseUrl}
 * @param wordmark        plain-text wordmark shown in the header
 * @param contact         support contact block for the footer
 * @param social          social profile URLs; a blank/null one hides that icon
 * @param preferencesPath path of the notification-preferences page (non-transactional footer link)
 */
@ConfigurationProperties("nadoumi.brand")
public record BrandProperties(
        String baseUrl,
        String logoPath,
        String wordmark,
        Contact contact,
        Social social,
        String preferencesPath) {

    public BrandProperties {
        baseUrl = stripTrailingSlash(baseUrl == null || baseUrl.isBlank() ? "http://localhost:3000" : baseUrl.trim());
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
        return baseUrl + logoPath;
    }

    public String preferencesUrl() {
        return baseUrl + preferencesPath;
    }

    /** Absolute URL for an app path such as {@code /scholarships}. */
    public String url(String path) {
        if (path == null || path.isBlank()) {
            return baseUrl;
        }
        return path.startsWith("http") ? path : baseUrl + (path.startsWith("/") ? path : "/" + path);
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
