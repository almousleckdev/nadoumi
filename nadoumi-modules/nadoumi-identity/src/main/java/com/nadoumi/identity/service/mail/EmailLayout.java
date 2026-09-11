package com.nadoumi.identity.service.mail;

import com.nadoumi.identity.service.mail.EmailContent.Block;
import com.nadoumi.identity.service.mail.EmailContent.CodeBlock;
import com.nadoumi.identity.service.mail.EmailContent.Cta;
import com.nadoumi.identity.service.mail.EmailContent.Divider;
import com.nadoumi.identity.service.mail.EmailContent.ItemGroup;
import com.nadoumi.identity.service.mail.EmailContent.KeyValues;
import com.nadoumi.identity.service.mail.EmailContent.ListItem;
import com.nadoumi.identity.service.mail.EmailContent.Paragraph;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The single source of the Nadoumi transactional-email design. Turns an
 * {@link EmailContent} into a responsive, accessible HTML document and a matching
 * plain-text alternative. Deliberately <b>not</b> a boxed/card layout: content
 * sits directly on the page, structure comes from type scale and hairline rules.
 *
 * <p>No templating engine — the markup is small, fixed, and needs table-based
 * layout plus inlined CSS to survive real email clients.</p>
 */
@Component
public class EmailLayout {

    private static final String FONT =
            "-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif";
    private static final String C_BG = "#ffffff";
    private static final String C_TEXT = "#3b3f45";
    private static final String C_HEAD = "#16181a";
    private static final String C_MUTED = "#5c616a";
    private static final String C_RULE = "#e3e5e8";
    private static final String C_LINK = "#1a56db";

    private final BrandProperties brand;

    public EmailLayout(BrandProperties brand) {
        this.brand = brand;
    }

    public EmailRender render(EmailContent content) {
        return new EmailRender(html(content), text(content));
    }

    // ------------------------------------------------------------------ HTML

    private String html(EmailContent c) {
        String lang = c.locale().getLanguage().isBlank() ? "en" : c.locale().getLanguage();
        StringBuilder b = new StringBuilder(4096);
        b.append("<!DOCTYPE html>\n<html lang=\"").append(lang)
                .append("\" xmlns:v=\"urn:schemas-microsoft-com:vml\" xmlns:o=\"urn:schemas-microsoft-com:office:office\">\n");
        b.append("<head>\n<meta charset=\"utf-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n")
                .append("<meta name=\"color-scheme\" content=\"light\">\n")
                .append("<meta name=\"supported-color-schemes\" content=\"light\">\n")
                .append("<title>").append(esc(c.heading())).append("</title>\n")
                .append("<style>\n")
                .append("@media (max-width:620px){.np{padding-left:16px!important;padding-right:16px!important}")
                .append(".h1{font-size:20px!important}}\n")
                .append("</style>\n</head>\n");
        b.append("<body class=\"bg\" style=\"margin:0;padding:0;background:").append(C_BG).append(";\">\n");
        b.append("<span style=\"display:none!important;visibility:hidden;opacity:0;height:0;width:0;overflow:hidden;mso-hide:all;\">")
                .append(esc(c.preheader())).append("</span>\n");
        b.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" class=\"bg\" style=\"background:")
                .append(C_BG).append(";\">\n<tr><td align=\"center\" class=\"np\" style=\"padding:0 24px;\">\n");
        b.append("<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width:600px;max-width:600px;\">\n");

        // header
        b.append("<tr><td class=\"hd\" style=\"padding:28px 0 0 0;font-family:").append(FONT)
                .append(";font-size:18px;font-weight:700;letter-spacing:.02em;color:").append(C_HEAD).append(";\">")
                .append(esc(brand.wordmark())).append("</td></tr>\n");
        b.append(ruleRow("16px 0 0 0"));

        // body
        b.append("<tr><td style=\"padding:28px 0 0 0;\">\n");
        b.append("<h1 class=\"h1 hd\" style=\"margin:0 0 16px 0;font-family:").append(FONT)
                .append(";font-size:22px;line-height:1.3;font-weight:600;color:").append(C_HEAD).append(";\">")
                .append(esc(c.heading())).append("</h1>\n");
        for (Block block : c.blocks()) {
            b.append(blockHtml(block));
        }
        if (c.cta() != null) {
            b.append(ctaHtml(c.cta()));
        }
        for (ItemGroup group : c.itemGroups()) {
            b.append(itemGroupHtml(group));
        }
        b.append("</td></tr>\n");

        // footer
        b.append(ruleRow("32px 0 0 0"));
        b.append("<tr><td class=\"mu\" style=\"padding:20px 0 0 0;font-family:").append(FONT)
                .append(";font-size:13px;line-height:1.6;color:").append(C_MUTED).append(";\">\n")
                .append(footerContactHtml()).append("</td></tr>\n");
        String social = socialRowHtml();
        if (!social.isEmpty()) {
            b.append("<tr><td align=\"center\" style=\"padding:16px 0 0 0;\">").append(social).append("</td></tr>\n");
        }
        b.append("<tr><td align=\"center\" style=\"padding:24px 0 0 0;\"><img src=\"").append(esc(brand.logoUrl()))
                .append("\" alt=\"").append(esc(brand.wordmark()))
                .append("\" width=\"120\" style=\"display:block;border:0;height:auto;\"></td></tr>\n");
        b.append("<tr><td align=\"center\" class=\"mu\" style=\"padding:12px 0 40px 0;font-family:").append(FONT)
                .append(";font-size:12px;line-height:1.6;color:").append(C_MUTED).append(";\">© ")
                .append(Year.now().getValue()).append(" ").append(esc(brand.wordmark()))
                .append(". International education platform.");
        if (c.showPreferencesLink()) {
            b.append("<br><a href=\"").append(esc(brand.preferencesUrl()))
                    .append("\" class=\"lk\" style=\"color:").append(C_LINK).append(";\">Manage your email preferences</a>");
        }
        b.append("</td></tr>\n");

        b.append("</table>\n</td></tr>\n</table>\n</body>\n</html>");
        return b.toString();
    }

    private String blockHtml(Block block) {
        if (block instanceof Paragraph p) {
            return "<p class=\"tx\" style=\"margin:0 0 16px 0;font-family:" + FONT
                    + ";font-size:15px;line-height:1.6;color:" + C_TEXT + ";\">" + esc(p.text()) + "</p>\n";
        }
        if (block instanceof CodeBlock code) {
            return "<p class=\"hd\" style=\"margin:18px 0;font-family:" + FONT
                    + ";font-size:28px;line-height:1.2;font-weight:700;letter-spacing:.25em;color:" + C_HEAD + ";\">"
                    + esc(code.value()) + "</p>\n";
        }
        if (block instanceof KeyValues kv) {
            StringBuilder t = new StringBuilder(
                    "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"margin:0 0 16px 0;\">\n");
            for (KeyValues.Entry e : kv.entries()) {
                t.append("<tr><td valign=\"top\" class=\"mu\" style=\"padding:0 12px 6px 0;font-family:").append(FONT)
                        .append(";font-size:13px;line-height:1.6;color:").append(C_MUTED).append(";\">").append(esc(e.label()))
                        .append("</td><td valign=\"top\" class=\"hd\" style=\"padding:0 0 6px 0;font-family:").append(FONT)
                        .append(";font-size:13px;line-height:1.6;color:").append(C_HEAD).append(";\">").append(esc(e.value()))
                        .append("</td></tr>\n");
            }
            return t.append("</table>\n").toString();
        }
        if (block instanceof Divider) {
            return "<div class=\"rule\" style=\"border-top:1px solid " + C_RULE
                    + ";font-size:0;line-height:0;margin:24px 0;\">&nbsp;</div>\n";
        }
        throw new IllegalStateException("unhandled block: " + block);
    }

    private String ctaHtml(Cta cta) {
        String url = esc(cta.url());
        String label = esc(cta.label());
        return "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"margin:8px 0 4px 0;\"><tr><td>\n"
                + "<!--[if mso]><v:roundrect xmlns:v=\"urn:schemas-microsoft-com:vml\" xmlns:w=\"urn:schemas-microsoft-com:office:word\" "
                + "href=\"" + url + "\" style=\"height:44px;v-text-anchor:middle;width:220px;\" arcsize=\"14%\" fillcolor=\"" + C_HEAD
                + "\" stroke=\"f\"><w:anchorlock/><center style=\"color:#ffffff;font-family:" + FONT
                + ";font-size:15px;font-weight:600;\">" + label + "</center></v:roundrect><![endif]-->\n"
                + "<!--[if !mso]><!-- --><a href=\"" + url + "\" class=\"btn\" style=\"background:" + C_HEAD
                + ";color:#ffffff;text-decoration:none;display:inline-block;padding:12px 22px;border-radius:6px;font-family:" + FONT
                + ";font-size:15px;font-weight:600;\">" + label + "</a><!--<![endif]-->\n"
                + "</td></tr></table>\n";
    }

    private String itemGroupHtml(ItemGroup group) {
        StringBuilder b = new StringBuilder("<div style=\"margin:24px 0 0 0;\">\n");
        b.append("<h2 class=\"hd\" style=\"margin:0 0 4px 0;font-family:").append(FONT)
                .append(";font-size:16px;line-height:1.4;font-weight:600;color:").append(C_HEAD).append(";\">")
                .append(esc(group.heading())).append("</h2>\n");
        for (ListItem item : group.items()) {
            b.append("<div class=\"rule\" style=\"border-top:1px solid ").append(C_RULE).append(";padding:14px 0;\">\n")
                    .append("<div class=\"hd\" style=\"font-family:").append(FONT)
                    .append(";font-size:15px;line-height:1.4;font-weight:600;color:").append(C_HEAD).append(";\">")
                    .append(esc(item.title())).append("</div>\n");
            if (item.meta() != null && !item.meta().isBlank()) {
                b.append("<div class=\"mu\" style=\"font-family:").append(FONT)
                        .append(";font-size:13px;line-height:1.5;color:").append(C_MUTED).append(";margin:2px 0 6px 0;\">")
                        .append(esc(item.meta())).append("</div>\n");
            }
            b.append("<a href=\"").append(esc(item.url())).append("\" class=\"lk\" style=\"font-family:").append(FONT)
                    .append(";font-size:13px;color:").append(C_LINK).append(";\">View</a>\n</div>\n");
        }
        return b.append("</div>\n").toString();
    }

    private String footerContactHtml() {
        BrandProperties.Contact c = brand.contact();
        List<String> lines = new ArrayList<>();
        if (c.officeEn() != null && !c.officeEn().isBlank()) {
            lines.add("<div>" + esc(c.officeEn()) + "</div>");
        }
        if (c.hours() != null && !c.hours().isBlank()) {
            lines.add("<div>" + esc(c.hours()) + "</div>");
        }
        if (!c.emails().isEmpty()) {
            lines.add("<div>" + joinLinks(c.emails(), "mailto:") + "</div>");
        }
        if (!c.phones().isEmpty()) {
            lines.add("<div>" + joinLinks(c.phones(), "tel:") + "</div>");
        }
        return lines.isEmpty() ? "<div>&nbsp;</div>\n" : String.join("\n", lines) + "\n";
    }

    private String joinLinks(List<String> values, String scheme) {
        List<String> parts = new ArrayList<>();
        for (String v : values) {
            String href = scheme.equals("tel:") ? "tel:" + v.replaceAll("\\s+", "") : scheme + v;
            parts.add("<a href=\"" + esc(href) + "\" class=\"lk\" style=\"color:" + C_LINK + ";text-decoration:none;\">"
                    + esc(v) + "</a>");
        }
        return String.join("&nbsp;&middot;&nbsp;", parts);
    }

    private String socialRowHtml() {
        BrandProperties.Social s = brand.social();
        StringBuilder b = new StringBuilder();
        appendIcon(b, s.facebook(), "facebook.png", "Facebook");
        appendIcon(b, s.instagram(), "instagram.png", "Instagram");
        appendIcon(b, s.tiktok(), "tiktok.png", "TikTok");
        appendIcon(b, s.whatsapp(), "whatsapp.png", "WhatsApp");
        return b.toString();
    }

    private void appendIcon(StringBuilder b, String url, String file, String name) {
        if (url == null || url.isBlank()) {
            return;
        }
        b.append("<a href=\"").append(esc(url)).append("\" style=\"display:inline-block;margin:0 6px;\"><img src=\"")
                .append(esc(brand.url("/email/" + file))).append("\" alt=\"").append(esc(brand.wordmark()))
                .append(" on ").append(esc(name))
                .append("\" width=\"24\" height=\"24\" style=\"display:block;border:0;\"></a>");
    }

    private String ruleRow(String padding) {
        return "<tr><td style=\"padding:" + padding + ";\"><div class=\"rule\" style=\"border-top:1px solid " + C_RULE
                + ";font-size:0;line-height:0;\">&nbsp;</div></td></tr>\n";
    }

    // ------------------------------------------------------------- plain text

    private String text(EmailContent c) {
        StringBuilder b = new StringBuilder(1024);
        if (!c.preheader().isBlank()) {
            b.append(wrap(c.preheader())).append("\n\n");
        }
        String h = c.heading().toUpperCase(c.locale());
        b.append(h).append('\n').append("=".repeat(h.length())).append("\n\n");
        for (Block block : c.blocks()) {
            b.append(blockText(block)).append('\n');
        }
        if (c.cta() != null) {
            b.append(c.cta().label()).append(": ").append(c.cta().url()).append("\n\n");
        }
        for (ItemGroup group : c.itemGroups()) {
            b.append(group.heading()).append('\n');
            for (ListItem item : group.items()) {
                b.append("- ").append(item.title());
                if (item.meta() != null && !item.meta().isBlank()) {
                    b.append(" · ").append(item.meta());
                }
                b.append(" · ").append(item.url()).append('\n');
            }
            b.append('\n');
        }
        b.append("--\n").append(brand.wordmark()).append('\n');
        BrandProperties.Contact ct = brand.contact();
        if (ct.officeEn() != null && !ct.officeEn().isBlank()) {
            b.append(wrap(ct.officeEn())).append('\n');
        }
        if (ct.hours() != null && !ct.hours().isBlank()) {
            b.append(ct.hours()).append('\n');
        }
        if (!ct.emails().isEmpty()) {
            b.append(String.join(" · ", ct.emails())).append('\n');
        }
        if (!ct.phones().isEmpty()) {
            b.append(String.join(" · ", ct.phones())).append('\n');
        }
        BrandProperties.Social s = brand.social();
        appendSocialLine(b, "Facebook", s.facebook());
        appendSocialLine(b, "Instagram", s.instagram());
        appendSocialLine(b, "TikTok", s.tiktok());
        appendSocialLine(b, "WhatsApp", s.whatsapp());
        b.append('\n').append("© ").append(Year.now().getValue()).append(' ').append(brand.wordmark())
                .append(". International education platform.\n");
        if (c.showPreferencesLink()) {
            b.append("Manage your email preferences: ").append(brand.preferencesUrl()).append('\n');
        }
        return b.toString();
    }

    private String blockText(Block block) {
        if (block instanceof Paragraph p) {
            return wrap(p.text()) + "\n";
        }
        if (block instanceof CodeBlock code) {
            return "CODE: " + code.value() + "\n";
        }
        if (block instanceof KeyValues kv) {
            StringBuilder t = new StringBuilder();
            for (KeyValues.Entry e : kv.entries()) {
                t.append(e.label()).append(": ").append(e.value()).append('\n');
            }
            return t.toString();
        }
        if (block instanceof Divider) {
            return "---\n";
        }
        throw new IllegalStateException("unhandled block: " + block);
    }

    private static void appendSocialLine(StringBuilder b, String name, String url) {
        if (url != null && !url.isBlank()) {
            b.append(name).append(": ").append(url).append('\n');
        }
    }

    /** Greedy word-wrap at 72 columns; existing newlines are preserved. */
    static String wrap(String text) {
        StringBuilder out = new StringBuilder();
        for (String line : text.strip().split("\n", -1)) {
            if (line.isBlank()) {
                out.append('\n');
                continue;
            }
            int col = 0;
            for (String word : line.strip().split("\\s+")) {
                if (col > 0 && col + 1 + word.length() > 72) {
                    out.append('\n');
                    col = 0;
                }
                if (col > 0) {
                    out.append(' ');
                    col++;
                }
                out.append(word);
                col += word.length();
            }
            out.append('\n');
        }
        // drop the single trailing newline this loop always adds
        if (out.length() > 0 && out.charAt(out.length() - 1) == '\n') {
            out.setLength(out.length() - 1);
        }
        return out.toString();
    }

    static String esc(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '&' -> b.append("&amp;");
                case '<' -> b.append("&lt;");
                case '>' -> b.append("&gt;");
                case '"' -> b.append("&quot;");
                case '\'' -> b.append("&#39;");
                default -> b.append(ch);
            }
        }
        return b.toString();
    }
}
