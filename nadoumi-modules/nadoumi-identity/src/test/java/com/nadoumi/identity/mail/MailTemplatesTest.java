package com.nadoumi.identity.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nadoumi.identity.service.mail.MailTemplates;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MailTemplatesTest {

    private final MailTemplates templates = new MailTemplates();

    @Test
    void renders_otp_register_with_the_code() {
        String out = templates.render("otp-register", Map.of("otp", "482913", "ttlMinutes", "10"));
        assertThat(out).contains("482913").contains("10 minutes");
    }

    @Test
    void rejects_an_unbound_placeholder() {
        assertThatThrownBy(() -> templates.render("otp-register", Map.of("ttlMinutes", "10")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("otp");
    }

    @Test
    void rejects_an_unknown_template() {
        assertThatThrownBy(() -> templates.render("does-not-exist", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }
}
