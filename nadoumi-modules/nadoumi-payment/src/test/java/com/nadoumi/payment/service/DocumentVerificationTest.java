package com.nadoumi.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nadoumi.identity.service.mail.BrandProperties;
import com.nadoumi.payment.domain.PaymentDocument;
import com.nadoumi.payment.domain.enums.DocumentKind;
import com.nadoumi.payment.mapper.PaymentDocumentMapper;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DocumentVerificationTest {

    private final PaymentDocumentMapper documents = mock(PaymentDocumentMapper.class);
    private final BrandProperties brand = new BrandProperties("https://nadoumi.test", null, null, "Nadoumi", null, null, null);
    private final DocumentVerification verification = new DocumentVerification(documents, brand);

    private static PaymentDocument issued(String number, String code) {
        PaymentDocument d = new PaymentDocument();
        d.setDocNumber(number);
        d.setVerifyCode(code);
        d.setKind(DocumentKind.RECEIPT);
        d.setGeneratedAt(LocalDateTime.of(2026, 10, 22, 6, 30));
        return d;
    }

    @Test
    void shouldGenerateTwelveReadableCharacters_thatDoNotRepeat() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            String code = verification.newCode();
            assertThat(code).hasSize(12).matches("[A-HJKMNP-Z2-9]{12}");
            seen.add(code);
        }
        assertThat(seen).hasSize(500);
    }

    @Test
    void shouldBuildTheAddressTheQrCodeOpens() {
        assertThat(verification.url("NAD-RCT-2026-000007", "ABCD2345EFGH"))
                .isEqualTo("https://nadoumi.test/verify?number=NAD-RCT-2026-000007&code=ABCD2345EFGH");
    }

    @Test
    void shouldVerifyOnlyTheMatchingNumberAndCode() {
        when(documents.findByNumber("NAD-RCT-2026-000007")).thenReturn(List.of(issued("NAD-RCT-2026-000007", "ABCD2345EFGH")));

        assertThat(verification.verify("NAD-RCT-2026-000007", "ABCD2345EFGH")).isPresent();
        assertThat(verification.verify("NAD-RCT-2026-000007", "abcd2345efgh")).isPresent(); // typed in lower case
        assertThat(verification.verify("NAD-RCT-2026-000007", "ZZZZ2345EFGH")).isEmpty();
        assertThat(verification.verify("NAD-RCT-2026-000099", "ABCD2345EFGH")).isEmpty();
    }

    @Test
    void shouldRefuseMalformedInputWithoutTouchingTheDatabase() {
        assertThat(verification.verify(null, "ABCD2345EFGH")).isEmpty();
        assertThat(verification.verify("NAD-RCT-2026-000007", null)).isEmpty();
        assertThat(verification.verify("NAD-RCT-2026-000007", "SHORT")).isEmpty();
        assertThat(verification.verify("x".repeat(100), "ABCD2345EFGH")).isEmpty();
        org.mockito.Mockito.verifyNoInteractions(documents);
    }
}
