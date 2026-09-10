package com.nadoumi.identity.mail;

import static org.assertj.core.api.Assertions.assertThat;

import com.nadoumi.identity.service.mail.BrandProperties;
import com.nadoumi.identity.service.mail.EmailContent;
import com.nadoumi.identity.service.mail.EmailContent.ListItem;
import com.nadoumi.identity.service.mail.EmailLayout;
import com.nadoumi.identity.service.mail.EmailRender;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EmailLayoutTest {

    private static BrandProperties brand(String facebook, String instagram, String tiktok) {
        return new BrandProperties(
                "https://nadoumi.test", "/email/nadoumi-logo.png", "Nadoumi",
                new BrandProperties.Contact(
                        List.of("support@nadoumi.test"), List.of("+86 159 0823 7607"),
                        "1 Test Road, Mianyang", "Mon-Fri, 09:00-18:00 UTC+8"),
                new BrandProperties.Social(facebook, instagram, tiktok),
                "/account/notifications");
    }

    private static EmailLayout layout(String fb, String ig, String tt) {
        return new EmailLayout(brand(fb, ig, tt));
    }

    private static EmailContent sample(boolean withPrefsLink) {
        return EmailContent.builder("Verify your email address")
                .preheader("Your Nadoumi verification code")
                .paragraph("Enter this code to verify your email address.")
                .code("482913")
                .paragraph("This code expires in 10 minutes.")
                .cta("Sign in", "https://nadoumi.test/login")
                .itemGroup("Programmes to explore", List.of(
                        new ListItem("MBBS — Fudan University", "Bachelor · Medicine",
                                "https://nadoumi.test/programs/mbbs-fudan")))
                .showPreferencesLink(withPrefsLink)
                .build();
    }

    @Test
    void html_carries_the_accessible_non_boxed_shell() {
        String html = layout(null, null, null).render(sample(true)).html();

        assertThat(html).startsWith("<!DOCTYPE html>");
        assertThat(html).contains("<html lang=\"en\"");
        assertThat(html).contains("<title>Verify your email address</title>");
        assertThat(html).contains("Your Nadoumi verification code");                 // preheader
        assertThat(html).contains("<h1");
        assertThat(html).contains("role=\"presentation\"");
        assertThat(html).contains("482913");
        assertThat(html).contains("https://nadoumi.test/login");                     // CTA
        assertThat(html).contains("https://nadoumi.test/programs/mbbs-fudan");       // list item
        assertThat(html).contains("<img src=\"https://nadoumi.test/email/nadoumi-logo.png\" alt=\"Nadoumi\"");
        assertThat(html).contains("support@nadoumi.test").contains("1 Test Road, Mianyang");
        assertThat(html).contains("Manage your email preferences");
        // not a boxed/card layout
        assertThat(html).doesNotContain("box-shadow");
    }

    @Test
    void plain_text_mirrors_every_url_and_the_code_from_the_html() {
        EmailRender r = layout(null, null, null).render(sample(true));

        assertThat(r.text()).contains("VERIFY YOUR EMAIL ADDRESS").contains("=====");
        assertThat(r.text()).contains("CODE: 482913");
        assertThat(r.text()).contains("Sign in: https://nadoumi.test/login");
        assertThat(r.text()).contains("Programmes to explore");
        assertThat(r.text()).contains("https://nadoumi.test/programs/mbbs-fudan");
        assertThat(r.text()).contains("support@nadoumi.test");
        assertThat(r.text()).contains("Manage your email preferences: https://nadoumi.test/account/notifications");

        // parity: URLs + code present in HTML are present in text
        for (String needle : List.of("482913", "https://nadoumi.test/login",
                "https://nadoumi.test/programs/mbbs-fudan", "support@nadoumi.test")) {
            assertThat(r.html()).contains(needle);
            assertThat(r.text()).contains(needle);
        }
    }

    @Test
    void transactional_content_has_no_preferences_link() {
        String html = layout(null, null, null).render(sample(false)).html();
        assertThat(html).doesNotContain("Manage your email preferences");
    }

    @Test
    void social_icons_render_only_for_configured_networks() {
        String none = layout(null, null, null).render(sample(true)).html();
        assertThat(none).doesNotContain("/email/facebook.png")
                .doesNotContain("/email/instagram.png").doesNotContain("/email/tiktok.png");

        String igOnly = layout(null, "https://instagram.com/nadoumi", null).render(sample(true)).html();
        assertThat(igOnly).contains("<img src=\"https://nadoumi.test/email/instagram.png\" alt=\"Nadoumi on Instagram\"");
        assertThat(igOnly).doesNotContain("/email/facebook.png").doesNotContain("/email/tiktok.png");

        String all = layout("https://facebook.com/n", "https://instagram.com/n", "https://tiktok.com/@n")
                .render(sample(true)).html();
        assertThat(all).contains("/email/facebook.png").contains("/email/instagram.png").contains("/email/tiktok.png");
    }

    @Test
    void empty_item_group_is_dropped() {
        EmailContent c = EmailContent.builder("Welcome to Nadoumi")
                .paragraph("Your account is ready.")
                .itemGroup("Programmes to explore", List.of())
                .build();
        assertThat(c.itemGroups()).isEmpty();
        assertThat(layout(null, null, null).render(c).html()).doesNotContain("Programmes to explore");
    }

    @ParameterizedTest(name = "{0} on {1} ≥ {2}:1")
    @CsvSource({
            // foreground, background, minimum ratio (WCAG AA) — mirrors EmailLayout's palette
            "#3b3f45,#f6f7f8,4.5",   // body text
            "#16181a,#f6f7f8,4.5",   // headings / code
            "#5c616a,#f6f7f8,4.5",   // muted footer text
            "#1a56db,#f6f7f8,4.5",   // links
            "#ffffff,#16181a,4.5",   // CTA button label on fill
    })
    void palette_meets_wcag_aa_contrast(String fg, String bg, double min) {
        assertThat(contrastRatio(fg, bg)).isGreaterThanOrEqualTo(min);
    }

    // --- WCAG 2.x relative-luminance contrast ratio -----------------------------

    private static double contrastRatio(String hexA, String hexB) {
        double la = relativeLuminance(hexA);
        double lb = relativeLuminance(hexB);
        double lighter = Math.max(la, lb);
        double darker = Math.min(la, lb);
        return (lighter + 0.05) / (darker + 0.05);
    }

    private static double relativeLuminance(String hex) {
        int[] rgb = {
                Integer.parseInt(hex.substring(1, 3), 16),
                Integer.parseInt(hex.substring(3, 5), 16),
                Integer.parseInt(hex.substring(5, 7), 16)
        };
        double[] lin = new double[3];
        for (int i = 0; i < 3; i++) {
            double c = rgb[i] / 255.0;
            lin[i] = c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * lin[0] + 0.7152 * lin[1] + 0.0722 * lin[2];
    }
}
