package com.nadoumi.applicant.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class PhotoNormalizerTest {

    private static byte[] encode(int width, int height, int type, String format) throws IOException {
        BufferedImage image = new BufferedImage(width, height, type);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }

    @Test
    void shouldShrinkABigPhotoToThePassportBounds_asAJpeg() throws IOException {
        byte[] jpeg = PhotoNormalizer.toJpeg(encode(3000, 4000, BufferedImage.TYPE_INT_RGB, "png")).orElseThrow();

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(jpeg));
        assertThat(result.getWidth()).isLessThanOrEqualTo(PhotoNormalizer.MAX_WIDTH);
        assertThat(result.getHeight()).isLessThanOrEqualTo(PhotoNormalizer.MAX_HEIGHT);
        assertThat(jpeg[0]).isEqualTo((byte) 0xFF); // JPEG signature
    }

    @Test
    void shouldNotEnlargeASmallPhoto() throws IOException {
        byte[] jpeg = PhotoNormalizer.toJpeg(encode(100, 130, BufferedImage.TYPE_INT_RGB, "png")).orElseThrow();

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(jpeg));
        assertThat(result.getWidth()).isEqualTo(100);
    }

    @Test
    void shouldAcceptATransparentPng() throws IOException {
        assertThat(PhotoNormalizer.toJpeg(encode(50, 50, BufferedImage.TYPE_INT_ARGB, "png"))).isPresent();
    }

    @Test
    void shouldGiveNoPhoto_forBytesThatAreNotAnImage() {
        assertThat(PhotoNormalizer.toJpeg(new byte[] {1, 2, 3})).isEmpty();
        assertThat(PhotoNormalizer.toJpeg("<svg onload=alert(1)>".getBytes())).isEmpty();
        assertThat(PhotoNormalizer.toJpeg(new byte[0])).isEmpty();
    }
}
