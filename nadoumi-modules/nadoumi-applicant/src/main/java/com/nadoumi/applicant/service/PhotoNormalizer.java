package com.nadoumi.applicant.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import javax.imageio.ImageIO;

/**
 * Re-encodes a stored profile photo as a bounded JPEG for a printed document. The bytes are decoded and drawn again,
 * so whatever a file claimed to be, only plain pixels reach the PDF. A format the JDK cannot decode (AVIF, WebP) or
 * a corrupt file gives no photo rather than an error.
 */
final class PhotoNormalizer {

    /** Passport-photo proportions (35 x 45 mm) at 300 dpi. */
    static final int MAX_WIDTH = 413;
    static final int MAX_HEIGHT = 531;
    private static final float QUALITY_SIDE_LIMIT = 8_000; // refuse absurd dimensions before allocating

    private PhotoNormalizer() {
    }

    static Optional<byte[]> toJpeg(byte[] original) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(original));
            if (source == null || source.getWidth() <= 0 || source.getHeight() <= 0
                    || source.getWidth() > QUALITY_SIDE_LIMIT || source.getHeight() > QUALITY_SIDE_LIMIT) {
                return Optional.empty();
            }
            double scale = Math.min(1.0, Math.min((double) MAX_WIDTH / source.getWidth(), (double) MAX_HEIGHT / source.getHeight()));
            int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
            BufferedImage rgb = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgb.createGraphics();
            try {
                g.setColor(Color.WHITE); // a transparent PNG becomes white, not black
                g.fillRect(0, 0, width, height);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(source, 0, 0, width, height, null);
            }
            finally {
                g.dispose();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            return ImageIO.write(rgb, "jpeg", out) ? Optional.of(out.toByteArray()) : Optional.empty();
        }
        catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }
}
