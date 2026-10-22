package com.nadoumi.payment.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.IntPredicate;

/** Turns values into the plain strings a PDF prints. Nothing here is markup: text is drawn, never parsed. */
final class PdfText {

    static final String EMPTY = "Not provided";
    private static final ZoneId CHINA = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT);
    private static final char REPLACEMENT = '?';

    private PdfText() {
    }

    /** Drops control characters and swaps any glyph the font lacks for '?', so a stray character can't break a page. */
    static String printable(String value, IntPredicate canDraw) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length());
        value.codePoints().forEach(cp -> {
            if (cp == '\n' || cp == '\t' || cp >= ' ' && cp != 0x7f) {
                out.appendCodePoint(cp == '\t' ? ' ' : canDraw.test(cp) || cp == '\n' ? cp : REPLACEMENT);
            }
        });
        return out.toString().strip();
    }

    /** "HIGH_SCHOOL" becomes "High school": a stored code worded for a reader. Null stays null. */
    static String label(String code) {
        if (code == null || code.isBlank()) {
            return code;
        }
        String words = code.strip().replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    static String orEmpty(String value) {
        return value == null || value.isBlank() ? EMPTY : value;
    }

    static String stamp(Instant instant) {
        return STAMP.format(instant.atZone(CHINA)) + " (UTC+8)";
    }

    static String money(BigDecimal amount, String currency) {
        String symbol = "CNY".equals(currency) ? "¥ " : currency + " ";
        return symbol + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
