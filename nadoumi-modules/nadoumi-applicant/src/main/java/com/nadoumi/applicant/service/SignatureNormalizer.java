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
 * Re-encodes a drawn or uploaded signature as a small PNG on a white background. The image is decoded and drawn
 * again, so only plain pixels are kept, and a picture with next to no ink (an empty pad) is rejected.
 */
final class SignatureNormalizer {

    static final int MAX_WIDTH = 600;
    static final int MAX_HEIGHT = 240;
    /** Fewer inked pixels than this is an empty pad, not a signature. */
    static final int MIN_INK_PIXELS = 40;
    private static final int SIDE_LIMIT = 6_000;
    private static final int NEAR_WHITE = 235;

    private SignatureNormalizer() {
    }

    /** The cleaned PNG, or empty when the bytes are not an image or the image is blank. */
    static Optional<byte[]> toPng(byte[] original) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(original));
            if (source == null || source.getWidth() <= 0 || source.getHeight() <= 0
                    || source.getWidth() > SIDE_LIMIT || source.getHeight() > SIDE_LIMIT) {
                return Optional.empty();
            }
            double scale = Math.min(1.0, Math.min((double) MAX_WIDTH / source.getWidth(), (double) MAX_HEIGHT / source.getHeight()));
            int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
            BufferedImage rgb = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgb.createGraphics();
            try {
                g.setColor(Color.WHITE); // a transparent pad becomes white, so ink is what is left
                g.fillRect(0, 0, width, height);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(source, 0, 0, width, height, null);
            }
            finally {
                g.dispose();
            }
            if (inkPixels(rgb) < MIN_INK_PIXELS) {
                return Optional.empty();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            return ImageIO.write(rgb, "png", out) ? Optional.of(out.toByteArray()) : Optional.empty();
        }
        catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    private static int inkPixels(BufferedImage image) {
        int ink = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int min = Math.min((rgb >> 16) & 0xFF, Math.min((rgb >> 8) & 0xFF, rgb & 0xFF));
                if (min < NEAR_WHITE) {
                    ink++;
                }
            }
        }
        return ink;
    }
}
