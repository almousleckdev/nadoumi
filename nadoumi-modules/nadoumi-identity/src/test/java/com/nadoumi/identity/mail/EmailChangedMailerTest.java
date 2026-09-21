package com.nadoumi.identity.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nadoumi.identity.service.mail.BrandProperties;
import com.nadoumi.identity.service.mail.EmailChangedEvent;
import com.nadoumi.identity.service.mail.EmailChangedMailer;
import com.nadoumi.identity.service.mail.EmailLayout;
import com.nadoumi.identity.service.mail.EmailMessage;
import com.nadoumi.identity.service.mail.MailSender;
import com.nadoumi.identity.service.mail.MailTemplates;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EmailChangedMailerTest {

    private final MailSender mail = mock(MailSender.class);
    private final BrandProperties brand = new BrandProperties(
            "https://nadoumi.test", null, null, "Nadoumi",
            new BrandProperties.Contact(List.of("support@nadoumi.test"), List.of(), null, null),
            new BrandProperties.Social(null, null, null, null), null);
    private final EmailChangedMailer mailer =
            new EmailChangedMailer(new MailTemplates(), new EmailLayout(brand), mail, brand);

    @Test
    void sends_a_branded_notice_to_the_previous_address_naming_the_new_one() {
        mailer.onEmailChanged(new EmailChangedEvent(42L, "old@nadoumi.test", "new@nadoumi.test",
                Instant.parse("2026-02-03T09:15:00Z")));

        ArgumentCaptor<EmailMessage> sent = ArgumentCaptor.forClass(EmailMessage.class);
        verify(mail).send(sent.capture());
        EmailMessage msg = sent.getValue();
        assertThat(msg.to()).isEqualTo("old@nadoumi.test");
        assertThat(msg.subject()).isEqualTo("Your Nadoumi sign-in email was changed");
        assertThat(msg.htmlBody()).contains("3 Feb 2026, 09:15 UTC")
                .contains("new@nadoumi.test")
                .contains("mailto:support@nadoumi.test");
    }

    @Test
    void a_send_failure_is_swallowed_so_it_cannot_undo_the_email_change() {
        doThrow(new RuntimeException("smtp down")).when(mail).send(any());

        assertThatCode(() -> mailer.onEmailChanged(
                new EmailChangedEvent(42L, "old@nadoumi.test", "new@nadoumi.test", Instant.now())))
                .doesNotThrowAnyException();
    }

    @Test
    void does_nothing_without_a_previous_address_to_notify() {
        mailer.onEmailChanged(new EmailChangedEvent(42L, null, "new@nadoumi.test", Instant.now()));
        verify(mail, never()).send(any());
    }
}
