package com.nadoumi.notification.channel;

import com.nadoumi.common.notification.NotificationChannel;
import com.nadoumi.common.notification.NotificationChannelKind;
import com.nadoumi.common.notification.NotificationSendResult;
import com.nadoumi.identity.service.mail.BrandProperties;
import com.nadoumi.identity.service.mail.EmailContent;
import com.nadoumi.identity.service.mail.EmailLayout;
import com.nadoumi.identity.service.mail.EmailMessage;
import com.nadoumi.identity.service.mail.EmailRender;
import com.nadoumi.identity.service.mail.MailSender;

/**
 * EMAIL delivery — bridges the {@link NotificationChannel} SPI to the existing
 * {@link MailSender} port. The rendered plain body is wrapped in the shared
 * {@link EmailLayout} so notification emails carry the same Nadoumi design as the
 * identity ones; a per-type call-to-action deep-links into the site or the admin
 * console. A thrown send is reported {@code FAILED} so the dispatcher retries.
 */
public class EmailNotificationChannel implements NotificationChannel {

    /** Reported on the delivery row; the concrete transport (smtp/log) is chosen below the port. */
    public static final String PROVIDER = "mail";

    private final MailSender mailSender;
    private final EmailLayout emailLayout;
    private final BrandProperties brand;

    public EmailNotificationChannel(MailSender mailSender, EmailLayout emailLayout, BrandProperties brand) {
        this.mailSender = mailSender;
        this.emailLayout = emailLayout;
        this.brand = brand;
    }

    @Override
    public NotificationChannelKind kind() {
        return NotificationChannelKind.EMAIL;
    }

    @Override
    public NotificationSendResult send(String recipient, String subject, String body, String type) {
        try {
            EmailContent.Builder builder = EmailContent.builder(subject == null || subject.isBlank() ? "Nadoumi" : subject)
                    .preheader(firstLine(body))
                    .paragraphs(body)
                    .showPreferencesLink(!isStaffType(type));
            Cta cta = ctaFor(type);
            if (cta != null) {
                builder.cta(cta.label(), cta.url());
            }
            EmailRender rendered = emailLayout.render(builder.build());
            mailSender.send(new EmailMessage(recipient, subject, rendered.text(), rendered.html()));
            return NotificationSendResult.sent(null); // the port yields no per-message id
        } catch (RuntimeException e) {
            return NotificationSendResult.failed(e.toString());
        }
    }

    private record Cta(String label, String url) {
    }

    private Cta ctaFor(String type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case "SCHOLARSHIP_PUBLISHED", "SCHOLARSHIP_DEADLINE_REMINDER" ->
                    new Cta("View scholarships", brand.url("/scholarships"));
            case "PROGRAM_PUBLISHED" -> new Cta("View programmes", brand.url("/programs"));
            case "UNIVERSITY_PUBLISHED" -> new Cta("View universities", brand.url("/universities"));
            case "APPLICATION_SUBMITTED", "APPLICATION_STATUS_CHANGED" ->
                    new Cta("View your application", brand.url("/account/applications"));
            case "CONTACT_INQUIRY_RECEIVED", "TASK_PROGRESS" ->
                    new Cta("Open the admin console", brand.url("/admin"));
            default -> null;
        };
    }

    private static boolean isStaffType(String type) {
        return "CONTACT_INQUIRY_RECEIVED".equals(type) || "TASK_PROGRESS".equals(type);
    }

    private static String firstLine(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        String trimmed = body.strip();
        int nl = trimmed.indexOf('\n');
        return nl < 0 ? trimmed : trimmed.substring(0, nl).strip();
    }
}
