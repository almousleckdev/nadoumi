package com.nadoumi.payment.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/** Generates the short reference a student writes in the transfer note, e.g. {@code NAD-7K3Q9FXT}. */
@Component
public class PaymentReferences {

    /** No 0/O or 1/I/L, which are easy to misread when copying from a phone. */
    private static final String ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final int LENGTH = 8;
    private static final String PREFIX = "NAD-";

    private final SecureRandom random = new SecureRandom();

    public String next() {
        StringBuilder sb = new StringBuilder(PREFIX);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
