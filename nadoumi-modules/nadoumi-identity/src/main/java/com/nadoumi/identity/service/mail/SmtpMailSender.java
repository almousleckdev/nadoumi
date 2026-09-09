package com.nadoumi.identity.service.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

/**
 * Production adapter. Active when {@code nadoumi.mail.transport=smtp}; requires
 * {@code spring.mail.host} (Mailpit locally, Gmail / SES / any SMTP in real
 * environments — configured entirely through the environment).
 */
@Component
@ConditionalOnProperty(name = "nadoumi.mail.transport", havingValue = "smtp")
public class SmtpMailSender implements MailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpMailSender.class);

    private final JavaMailSender mail;
    private final String from;

    public SmtpMailSender(JavaMailSender mail, @Value("${nadoumi.mail.from}") String from) {
        this.mail = mail;
        this.from = from;
        logEffectiveConfig();
    }

    @Override
    public void send(EmailMessage message) {
        SimpleMailMessage smtp = new SimpleMailMessage();
        smtp.setFrom(from);
        smtp.setTo(message.to());
        smtp.setSubject(message.subject());
        smtp.setText(message.body());
        try {
            mail.send(smtp);
        }
        catch (MailException e) {
            // Body is omitted on purpose (it carries the raw OTP). The cause is
            // logged so operators can tell an auth failure from a refused host;
            // NadApiExceptionHandler turns this into a 502 for the caller.
            log.error("SMTP send failed to={} subject=\"{}\"", message.to(), message.subject(), e);
            throw e;
        }
    }

    /** One line at startup so a misconfigured environment is obvious in the logs. */
    private void logEffectiveConfig() {
        if (mail instanceof JavaMailSenderImpl impl) {
            log.info("SMTP mail transport active: host={} port={} auth={} from={}",
                    impl.getHost(), impl.getPort(),
                    impl.getUsername() != null && !impl.getUsername().isBlank(), from);
        }
        else {
            log.info("SMTP mail transport active: from={}", from);
        }
    }
}
