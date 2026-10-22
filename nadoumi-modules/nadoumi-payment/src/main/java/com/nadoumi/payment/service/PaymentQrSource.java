package com.nadoumi.payment.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * The payment QR code image. It is a static China Construction Bank merchant code (it carries no amount or
 * order), bundled with the application as a classpath resource and served only inside an open payment session,
 * never as a public file. It is deliberately not stored in MySQL (file contents stay out of the database).
 */
@Component
public class PaymentQrSource {

    /** The image bytes and their media type. */
    public record QrImage(byte[] bytes, String contentType) {
    }

    private static final String RESOURCE = "payment/paymentqr.png";
    private static final String CONTENT_TYPE = "image/png";

    private volatile QrImage cached;

    public QrImage load() {
        QrImage image = cached;
        if (image == null) {
            try (InputStream in = new ClassPathResource(RESOURCE).getInputStream()) {
                image = new QrImage(in.readAllBytes(), CONTENT_TYPE);
            }
            catch (IOException e) {
                throw new UncheckedIOException("payment QR image is missing from the application", e);
            }
            cached = image;
        }
        return image;
    }
}
