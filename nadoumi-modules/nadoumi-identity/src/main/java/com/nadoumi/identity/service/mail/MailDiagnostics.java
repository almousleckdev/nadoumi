package com.nadoumi.identity.service.mail;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

/**
 * Read-only view of the effective outbound-mail wiring plus a one-shot test send,
 * so an operator can tell "transport is still {@code log}" apart from "SMTP auth
 * is failing" without shell access to the host. Never exposes the SMTP password.
 */
@Service
public class MailDiagnostics {

    private final MailSender mailSender;
    private final ObjectProvider<JavaMailSender> javaMailSender;
    private final String transport;
    private final String from;

    public MailDiagnostics(MailSender mailSender, ObjectProvider<JavaMailSender> javaMailSender,
            @Value("${nadoumi.mail.transport:log}") String transport,
            @Value("${nadoumi.mail.from:}") String from) {
        this.mailSender = mailSender;
        this.javaMailSender = javaMailSender;
        this.transport = transport;
        this.from = from;
    }

    /**
     * @param transport       the configured {@code nadoumi.mail.transport} ({@code log} or {@code smtp})
     * @param adapter          the {@link MailSender} bean actually in use
     * @param from             the {@code From:} address
     * @param smtpHost         SMTP host, {@code null} unless a real SMTP sender is wired
     * @param smtpPort         SMTP port
     * @param smtpAuthConfigured whether an SMTP username is set (password never reported)
     * @param willActuallySend whether a send leaves this process (i.e. transport is {@code smtp})
     */
    public record MailConfig(String transport, String adapter, String from, String smtpHost,
            Integer smtpPort, boolean smtpAuthConfigured, boolean willActuallySend) {
    }

    /** @param error {@code "<ExceptionClass>: <message>"} on failure, {@code null} on success */
    public record TestResult(boolean ok, String error) {
    }

    public MailConfig config() {
        String adapter = mailSender.getClass().getSimpleName();
        String host = null;
        Integer port = null;
        boolean authConfigured = false;
        if (javaMailSender.getIfAvailable() instanceof JavaMailSenderImpl impl && isSmtp()) {
            host = impl.getHost();
            port = impl.getPort();
            authConfigured = impl.getUsername() != null && !impl.getUsername().isBlank();
        }
        return new MailConfig(transport, adapter, from, host, port, authConfigured, isSmtp());
    }

    public TestResult sendTest(String to) {
        try {
            mailSender.send(new EmailMessage(to, "Nadoumi mail test",
                    "This is a Nadoumi outbound-email test. If it reached your inbox, SMTP is working."));
            return new TestResult(true, null);
        }
        catch (RuntimeException e) {
            String message = e.getMessage() == null ? "" : ": " + e.getMessage();
            return new TestResult(false, e.getClass().getSimpleName() + message);
        }
    }

    private boolean isSmtp() {
        return "smtp".equalsIgnoreCase(transport);
    }
}
