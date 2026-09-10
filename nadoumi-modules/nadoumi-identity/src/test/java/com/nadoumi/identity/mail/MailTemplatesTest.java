package com.nadoumi.identity.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nadoumi.identity.service.mail.MailTemplates;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MailTemplatesTest {

    private final MailTemplates templates = new MailTemplates();

    @Test
    void renders_otp_register_prose_without_placeholders() {
        String out = templates.render("otp-register", Map.of());
        assertThat(out).contains("verify your email address");
    }

    @Test
    void substitutes_a_bound_placeholder() {
        String out = templates.render("password-changed", Map.of("changedAt", "3 Feb 2026, 09:15 UTC"));
        assertThat(out).contains("3 Feb 2026, 09:15 UTC");
    }

    @Test
    void rejects_an_unbound_placeholder() {
        assertThatThrownBy(() -> templates.render("password-changed", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("changedAt");
    }

    @Test
    void rejects_an_unknown_template() {
        assertThatThrownBy(() -> templates.render("does-not-exist", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }
}
