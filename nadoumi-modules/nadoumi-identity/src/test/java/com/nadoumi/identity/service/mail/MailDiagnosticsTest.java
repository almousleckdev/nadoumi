package com.nadoumi.identity.service.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

class MailDiagnosticsTest {

    private final MailSender sender = mock(MailSender.class);

    @SuppressWarnings("unchecked")
    private static ObjectProvider<JavaMailSender> provider(JavaMailSender jms) {
        ObjectProvider<JavaMailSender> p = mock(ObjectProvider.class);
        when(p.getIfAvailable()).thenReturn(jms);
        return p;
    }

    @Test
    void config_reports_log_transport_as_not_actually_sending() {
        var diag = new MailDiagnostics(sender, provider(null), "log", "no-reply@nadoumi.local");

        var cfg = diag.config();

        assertThat(cfg.transport()).isEqualTo("log");
        assertThat(cfg.willActuallySend()).isFalse();
        assertThat(cfg.smtpHost()).isNull();
    }

    @Test
    void config_reports_smtp_host_and_auth_without_the_password() {
        JavaMailSenderImpl jms = new JavaMailSenderImpl();
        jms.setHost("smtp.gmail.com");
        jms.setPort(587);
        jms.setUsername("nadoumiedu@gmail.com");
        jms.setPassword("super-secret");
        var diag = new MailDiagnostics(sender, provider(jms), "smtp", "nadoumiedu@gmail.com");

        var cfg = diag.config();

        assertThat(cfg.willActuallySend()).isTrue();
        assertThat(cfg.smtpHost()).isEqualTo("smtp.gmail.com");
        assertThat(cfg.smtpPort()).isEqualTo(587);
        assertThat(cfg.smtpAuthConfigured()).isTrue();
        assertThat(cfg.toString()).doesNotContain("super-secret");
    }

    @Test
    void sendTest_returns_ok_on_success() {
        var diag = new MailDiagnostics(sender, provider(null), "smtp", "f@x.com");
        assertThat(diag.sendTest("a@x.com").ok()).isTrue();
    }

    @Test
    void sendTest_returns_the_failure_class_and_message() {
        doThrow(new MailAuthenticationException("Authentication failed")).when(sender).send(any(EmailMessage.class));
        var diag = new MailDiagnostics(sender, provider(null), "smtp", "f@x.com");

        var res = diag.sendTest("a@x.com");

        assertThat(res.ok()).isFalse();
        assertThat(res.error()).isEqualTo("MailAuthenticationException: Authentication failed");
    }
}
