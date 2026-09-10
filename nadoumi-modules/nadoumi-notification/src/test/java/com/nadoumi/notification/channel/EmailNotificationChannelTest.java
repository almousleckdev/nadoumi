package com.nadoumi.notification.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.nadoumi.common.notification.NotificationSendStatus;
import com.nadoumi.identity.service.mail.BrandProperties;
import com.nadoumi.identity.service.mail.EmailLayout;
import com.nadoumi.identity.service.mail.EmailMessage;
import com.nadoumi.identity.service.mail.MailSender;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EmailNotificationChannelTest {

    private final MailSender mailSender = mock(MailSender.class);
    private final BrandProperties brand = new BrandProperties(
            "https://nadoumi.test", null, "Nadoumi",
            new BrandProperties.Contact(List.of("support@nadoumi.test"), List.of(), null, null),
            new BrandProperties.Social(null, null, null), null);
    private final EmailNotificationChannel channel =
            new EmailNotificationChannel(mailSender, new EmailLayout(brand), brand);

    @Test
    void wraps_the_body_in_the_shell_and_keeps_a_plain_text_alternative() {
        var result = channel.send("stu@nadoumi.test", "New scholarship: X", "A new scholarship is available.",
                "SCHOLARSHIP_PUBLISHED");

        assertThat(result.status()).isEqualTo(NotificationSendStatus.SENT);
        ArgumentCaptor<EmailMessage> sent = ArgumentCaptor.forClass(EmailMessage.class);
        verify(mailSender).send(sent.capture());
        EmailMessage msg = sent.getValue();
        assertThat(msg.to()).isEqualTo("stu@nadoumi.test");
        assertThat(msg.subject()).isEqualTo("New scholarship: X");
        assertThat(msg.body()).contains("A new scholarship is available.");            // plain text
        assertThat(msg.htmlBody()).contains("<!DOCTYPE html>")
                .contains("A new scholarship is available.")
                .contains("https://nadoumi.test/scholarships")                         // type CTA
                .contains("Manage your email preferences");                           // non-transactional
    }

    @Test
    void staff_types_get_the_admin_cta_and_no_preferences_link() {
        channel.send("ops@nadoumi.test", "New contact inquiry #12", "A new inquiry was submitted.",
                "CONTACT_INQUIRY_RECEIVED");

        ArgumentCaptor<EmailMessage> sent = ArgumentCaptor.forClass(EmailMessage.class);
        verify(mailSender).send(sent.capture());
        assertThat(sent.getValue().htmlBody()).contains("https://nadoumi.test/admin")
                .doesNotContain("Manage your email preferences");
    }

    @Test
    void a_send_failure_is_reported_as_FAILED_for_retry() {
        doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(EmailMessage.class));

        var result = channel.send("stu@nadoumi.test", "s", "b", "PROGRAM_PUBLISHED");

        assertThat(result.status()).isEqualTo(NotificationSendStatus.FAILED);
        assertThat(result.error()).contains("smtp down");
    }
}
