package com.nadoumi.identity.service.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Production adapter. Active when {@code nadoumi.mail.transport=smtp}; requires
 * {@code spring.mail.host} (Mailpit locally, Gmail / SES / any SMTP in real
 * environments — configured entirely through the environment).
 */
@Component
@ConditionalOnProperty(name = "nadoumi.mail.transport", havingValue = "smtp")
public class SmtpMailSender implements MailSender {

    private final JavaMailSender mail;
    private final String from;

    public SmtpMailSender(JavaMailSender mail, @Value("${nadoumi.mail.from}") String from) {
        this.mail = mail;
        this.from = from;
    }

    @Override
    public void send(EmailMessage message) {
        SimpleMailMessage smtp = new SimpleMailMessage();
        smtp.setFrom(from);
        smtp.setTo(message.to());
        smtp.setSubject(message.subject());
        smtp.setText(message.body());
        mail.send(smtp);
    }
}
