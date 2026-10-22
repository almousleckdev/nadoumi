package com.nadoumi.applicant.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class SignatureNormalizerTest {

    private static byte[] png(int width, int height, boolean ink, int type) throws IOException {
        BufferedImage image = new BufferedImage(width, height, type);
        var g = image.createGraphics();
        if (type == BufferedImage.TYPE_INT_RGB) {
            g.setColor(Color.WHITE); // an RGB image starts black
            g.fillRect(0, 0, width, height);
        }
        if (ink) {
            g.setColor(Color.BLACK);
            g.setStroke(new java.awt.BasicStroke(5f));
            g.drawLine(10, height - 10, width - 10, 10);
        }
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    @Test
    void shouldKeepAnInkedSignature_asABoundedPng() throws IOException {
        byte[] result = SignatureNormalizer.toPng(png(1800, 600, true, BufferedImage.TYPE_INT_ARGB)).orElseThrow();

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(result));
        assertThat(image.getWidth()).isLessThanOrEqualTo(SignatureNormalizer.MAX_WIDTH);
        assertThat(image.getHeight()).isLessThanOrEqualTo(SignatureNormalizer.MAX_HEIGHT);
        assertThat(result[1]).isEqualTo((byte) 'P'); // PNG signature
    }

    @Test
    void shouldRejectAnEmptyPad_whetherWhiteOrTransparent() throws IOException {
        assertThat(SignatureNormalizer.toPng(png(600, 200, false, BufferedImage.TYPE_INT_RGB))).isEmpty();
        assertThat(SignatureNormalizer.toPng(png(600, 200, false, BufferedImage.TYPE_INT_ARGB))).isEmpty();
    }

    @Test
    void shouldRejectBytesThatAreNotAnImage() {
        assertThat(SignatureNormalizer.toPng(new byte[] {1, 2, 3})).isEmpty();
        assertThat(SignatureNormalizer.toPng("<script>alert(1)</script>".getBytes())).isEmpty();
    }
}
