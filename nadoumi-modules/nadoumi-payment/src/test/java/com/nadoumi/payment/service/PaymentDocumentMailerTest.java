package com.nadoumi.payment.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PaymentDocumentMailerTest {

    @Test
    void shouldStripLineBreaksAndControlCharacters_fromAHeaderValue() {
        String header = PaymentDocumentMailer.header("[Nadoumi] New application\r\nBcc: evil@example.test\u0000 : Amina");

        assertThat(header).doesNotContain("\r").doesNotContain("\n").doesNotContain("\u0000");
        assertThat(header).startsWith("[Nadoumi] New application");
    }

    @Test
    void shouldBoundTheLengthOfAHeaderValue() {
        assertThat(PaymentDocumentMailer.header("x".repeat(1000)).length()).isLessThanOrEqualTo(150);
    }
}
