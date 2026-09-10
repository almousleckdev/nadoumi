package com.nadoumi.identity.service.mail;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends the "your password was changed" security notice after a password reset or
 * change commits. It is a courtesy alert, not a gate: a send failure is logged
 * and swallowed so it can never undo the password change or the session revoke.
 */
@Component
public class PasswordChangedMailer {

    private static final Logger log = LoggerFactory.getLogger(PasswordChangedMailer.class);

    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private final MailTemplates templates;
    private final EmailLayout emailLayout;
    private final MailSender mail;
    private final BrandProperties brand;

    public PasswordChangedMailer(MailTemplates templates, EmailLayout emailLayout, MailSender mail,
            BrandProperties brand) {
        this.templates = templates;
        this.emailLayout = emailLayout;
        this.mail = mail;
        this.brand = brand;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordChanged(PasswordChangedEvent event) {
        if (event.email() == null || event.email().isBlank()) {
            return;
        }
        try {
            mail.send(compose(event));
        }
        catch (RuntimeException e) {
            log.warn("could not send the password-changed notice to userId={}", event.userId(), e);
        }
    }

    private EmailMessage compose(PasswordChangedEvent event) {
        String prose = templates.render("password-changed",
                Map.of("changedAt", WHEN.format(event.changedAt())));
        EmailContent.Builder builder = EmailContent.builder("Your password was changed")
                .preheader("A password change was made on your Nadoumi account")
                .paragraphs(prose);
        List<String> supportEmails = brand.contact().emails();
        if (!supportEmails.isEmpty()) {
            builder.cta("Contact support", "mailto:" + supportEmails.get(0));
        }
        EmailRender r = emailLayout.render(builder.build());
        return new EmailMessage(event.email(), "Your Nadoumi password was changed", r.text(), r.html());
    }
}
