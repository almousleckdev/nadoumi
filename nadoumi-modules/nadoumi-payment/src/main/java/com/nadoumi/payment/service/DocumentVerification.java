package com.nadoumi.payment.service;

import com.nadoumi.identity.service.mail.BrandProperties;
import com.nadoumi.payment.domain.PaymentDocument;
import com.nadoumi.payment.mapper.PaymentDocumentMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Lets anyone holding a printed receipt or form check that Nadoumi issued it. Each document carries a random,
 * unguessable code (not derived from the file, so it can be printed inside it) in a QR code that opens the public
 * verification page. The answer reveals only that the pair is valid and what kind of document it is, never who it is for.
 */
@Component
public class DocumentVerification {

    /** Letters and digits without the look-alikes (0/O, 1/I): easy to read aloud from a printout. */
    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    static final int CODE_LENGTH = 12;

    private final SecureRandom random = new SecureRandom();
    private final PaymentDocumentMapper documents;
    private final BrandProperties brand;

    public DocumentVerification(PaymentDocumentMapper documents, BrandProperties brand) {
        this.documents = documents;
        this.brand = brand;
    }

    public String newCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }

    /** The address the QR code opens. */
    public String url(String documentNumber, String code) {
        return brand.url("/verify?number=" + URLEncoder.encode(documentNumber, StandardCharsets.UTF_8)
                + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8));
    }

    /** The issued document when the number and code match, compared in constant time; empty otherwise. */
    public Optional<PaymentDocument> verify(String documentNumber, String code) {
        if (documentNumber == null || code == null || documentNumber.length() > 40 || code.length() != CODE_LENGTH) {
            return Optional.empty();
        }
        List<PaymentDocument> issued = documents.findByNumber(documentNumber.strip());
        byte[] given = code.strip().toUpperCase(java.util.Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        return issued.stream()
                .filter(d -> MessageDigest.isEqual(given, d.getVerifyCode().getBytes(StandardCharsets.UTF_8)))
                .findFirst();
    }
}
