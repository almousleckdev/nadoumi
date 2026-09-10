package com.nadoumi.identity.service.mail;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The channel-neutral model of a transactional email. {@link EmailLayout} turns
 * one {@code EmailContent} into both the HTML document and its plain-text
 * alternative, so the two can never drift.
 *
 * <p>Only {@code heading} and a non-empty {@code blocks} list are required.
 * Build one with {@link #builder(String)}.</p>
 */
public record EmailContent(
        String preheader,
        String heading,
        List<Block> blocks,
        Cta cta,
        List<ItemGroup> itemGroups,
        Locale locale,
        boolean showPreferencesLink) {

    public EmailContent {
        if (heading == null || heading.isBlank()) {
            throw new IllegalArgumentException("email content needs a heading");
        }
        if (blocks == null || blocks.isEmpty()) {
            throw new IllegalArgumentException("email content needs at least one block");
        }
        blocks = List.copyOf(blocks);
        itemGroups = itemGroups == null ? List.of() : List.copyOf(itemGroups);
        locale = locale == null ? Locale.ENGLISH : locale;
        preheader = preheader == null ? "" : preheader;
    }

    /** A body block. Sealed so {@link EmailLayout} handles every kind explicitly. */
    public sealed interface Block permits Paragraph, CodeBlock, KeyValues, Divider {
    }

    /** A prose paragraph. */
    public record Paragraph(String text) implements Block {
    }

    /** A short code shown large and letter-spaced on its own line (the OTP). */
    public record CodeBlock(String value) implements Block {
    }

    /** A borderless label/value list (inquiry details, an application summary). */
    public record KeyValues(List<Entry> entries) implements Block {
        public KeyValues {
            entries = List.copyOf(entries);
        }

        public record Entry(String label, String value) {
        }
    }

    /** A hairline rule between body sections. */
    public record Divider() implements Block {
    }

    /** The single call-to-action button. Its {@code url} is also shown in plain text. */
    public record Cta(String label, String url) {
    }

    /** A titled list of links, rendered as plain hairline-separated rows (never cards). */
    public record ItemGroup(String heading, List<ListItem> items) {
        public ItemGroup {
            items = List.copyOf(items);
        }
    }

    /** One row in an {@link ItemGroup}: a bold title, a one-line meta, a "View" link. */
    public record ListItem(String title, String meta, String url) {
    }

    public static Builder builder(String heading) {
        return new Builder(heading);
    }

    /** Fluent builder — keeps call sites readable. */
    public static final class Builder {
        private final String heading;
        private final List<Block> blocks = new ArrayList<>();
        private final List<ItemGroup> itemGroups = new ArrayList<>();
        private String preheader = "";
        private Cta cta;
        private Locale locale = Locale.ENGLISH;
        private boolean showPreferencesLink;

        private Builder(String heading) {
            this.heading = heading;
        }

        public Builder preheader(String preheader) {
            this.preheader = preheader == null ? "" : preheader;
            return this;
        }

        public Builder paragraph(String text) {
            blocks.add(new Paragraph(text));
            return this;
        }

        /** Adds each blank-line-separated span of {@code text} as its own paragraph. */
        public Builder paragraphs(String text) {
            if (text == null || text.isBlank()) {
                return this;
            }
            for (String part : text.strip().split("\\r?\\n\\s*\\r?\\n")) {
                String trimmed = part.strip();
                if (!trimmed.isEmpty()) {
                    blocks.add(new Paragraph(trimmed.replaceAll("\\s*\\r?\\n\\s*", " ")));
                }
            }
            return this;
        }

        public Builder code(String value) {
            blocks.add(new CodeBlock(value));
            return this;
        }

        public Builder keyValues(List<KeyValues.Entry> entries) {
            blocks.add(new KeyValues(entries));
            return this;
        }

        public Builder divider() {
            blocks.add(new Divider());
            return this;
        }

        public Builder cta(String label, String url) {
            this.cta = new Cta(label, url);
            return this;
        }

        public Builder cta(Cta cta) {
            this.cta = cta;
            return this;
        }

        /** Adds the group only when it has at least one item — empty sections are dropped. */
        public Builder itemGroup(String heading, List<ListItem> items) {
            if (items != null && !items.isEmpty()) {
                itemGroups.add(new ItemGroup(heading, items));
            }
            return this;
        }

        public Builder locale(Locale locale) {
            this.locale = locale == null ? Locale.ENGLISH : locale;
            return this;
        }

        public Builder showPreferencesLink(boolean show) {
            this.showPreferencesLink = show;
            return this;
        }

        public EmailContent build() {
            return new EmailContent(preheader, heading, blocks, cta, itemGroups, locale, showPreferencesLink);
        }
    }
}
