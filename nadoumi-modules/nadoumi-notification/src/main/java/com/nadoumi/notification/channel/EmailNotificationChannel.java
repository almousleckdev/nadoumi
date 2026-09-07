package com.nadoumi.notification.channel;

import com.nadoumi.common.notification.NotificationChannel;
import com.nadoumi.common.notification.NotificationChannelKind;
import com.nadoumi.common.notification.NotificationSendResult;
import com.nadoumi.identity.service.mail.EmailMessage;
import com.nadoumi.identity.service.mail.MailSender;

/**
 * EMAIL delivery — bridges the {@link NotificationChannel} SPI to the existing
 * {@link MailSender} port (SMTP in prod/staging, no-network log adapter in
 * dev/CI). A thrown send is reported as {@code FAILED} so the dispatcher retries
 * with backoff rather than losing the delivery.
 */
public class EmailNotificationChannel implements NotificationChannel {

    /** Reported on the delivery row; the concrete transport (smtp/log) is chosen below the port. */
    public static final String PROVIDER = "mail";

    private final MailSender mailSender;

    public EmailNotificationChannel(MailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public NotificationChannelKind kind() {
        return NotificationChannelKind.EMAIL;
    }

    @Override
    public NotificationSendResult send(String recipient, String subject, String body) {
        try {
            mailSender.send(new EmailMessage(recipient, subject, body));
            return NotificationSendResult.sent(null); // the port yields no per-message id
        } catch (RuntimeException e) {
            return NotificationSendResult.failed(e.toString());
        }
    }
}
