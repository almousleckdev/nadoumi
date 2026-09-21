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
 * Sends the "your sign-in email was changed" security notice, to the previous
 * address, after the change commits. It is a courtesy alert, not a gate: a send
 * failure is logged and swallowed so it can never undo the email change.
 */
@Component
public class EmailChangedMailer {

    private static final Logger log = LoggerFactory.getLogger(EmailChangedMailer.class);

    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private final MailTemplates templates;
    private final EmailLayout emailLayout;
    private final MailSender mail;
    private final BrandProperties brand;

    public EmailChangedMailer(MailTemplates templates, EmailLayout emailLayout, MailSender mail,
            BrandProperties brand) {
        this.templates = templates;
        this.emailLayout = emailLayout;
        this.mail = mail;
        this.brand = brand;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEmailChanged(EmailChangedEvent event) {
        if (event.notifyEmail() == null || event.notifyEmail().isBlank()) {
            return;
        }
        try {
            mail.send(compose(event));
        }
        catch (RuntimeException e) {
            log.warn("could not send the email-changed notice to userId={}", event.userId(), e);
        }
    }

    private EmailMessage compose(EmailChangedEvent event) {
        String prose = templates.render("email-changed",
                Map.of("newEmail", event.newEmail(), "changedAt", WHEN.format(event.changedAt())));
        EmailContent.Builder builder = EmailContent.builder("Your sign-in email was changed")
                .preheader("A sign-in email change was made on your Nadoumi account")
                .paragraphs(prose);
        List<String> supportEmails = brand.contact().emails();
        if (!supportEmails.isEmpty()) {
            builder.cta("Contact support", "mailto:" + supportEmails.get(0));
        }
        EmailRender r = emailLayout.render(builder.build());
        return new EmailMessage(event.notifyEmail(), "Your Nadoumi sign-in email was changed", r.text(), r.html());
    }
}
