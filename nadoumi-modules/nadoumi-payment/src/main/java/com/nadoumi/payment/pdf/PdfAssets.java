package com.nadoumi.payment.pdf;

import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.BaseFont;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import org.springframework.stereotype.Component;

/**
 * The fonts and logo every generated PDF uses. They are classpath resources only; nothing is fetched from disk
 * outside the jar or from a URL, so a document can never pull in a remote file.
 */
@Component
public class PdfAssets {

    private static final String BASE = "/payment/pdf/";

    private final BaseFont regular;
    private final BaseFont bold;
    private final BaseFont cjk;
    private final byte[] logo;

    public PdfAssets() {
        this.regular = font("NotoSans-Regular.ttf");
        this.bold = font("NotoSans-Bold.ttf");
        this.cjk = font("NadoumiSansSC-Regular.ttf");
        this.logo = read("logo.png");
    }

    public Font regular(float size, Color color) {
        return new Font(regular, size, Font.NORMAL, color);
    }

    public Font bold(float size, Color color) {
        return new Font(bold, size, Font.NORMAL, color);
    }

    /**
     * Text in one style that falls back, character by character, to the Chinese font when the Latin font has no glyph:
     * a label can carry its Chinese translation and a student's Chinese name still prints.
     */
    public com.lowagie.text.Phrase phrase(String text, float size, boolean bold, Color color) {
        com.lowagie.text.pdf.FontSelector selector = new com.lowagie.text.pdf.FontSelector();
        selector.addFont(bold ? bold(size, color) : regular(size, color));
        selector.addFont(new Font(cjk, size, Font.NORMAL, color));
        return selector.process(text);
    }

    /** A fresh instance each time: an {@link Image} carries position state, so it is never shared. */
    public Image logo() {
        try {
            return Image.getInstance(logo);
        }
        catch (IOException e) {
            throw new UncheckedIOException("bundled logo is unreadable", e);
        }
    }

    /** Whether one of the bundled fonts has a glyph for the code point. */
    public boolean canDraw(int codePoint) {
        return codePoint <= Character.MAX_VALUE
                && (regular.charExists((char) codePoint) || cjk.charExists((char) codePoint));
    }

    private static BaseFont font(String name) {
        try {
            return BaseFont.createFont(name, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, read(name), null);
        }
        catch (Exception e) {
            throw new IllegalStateException("bundled font is unusable: " + name, e);
        }
    }

    private static byte[] read(String name) {
        try (InputStream in = PdfAssets.class.getResourceAsStream(BASE + name)) {
            if (in == null) {
                throw new IllegalStateException("missing PDF resource " + name);
            }
            return in.readAllBytes();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
