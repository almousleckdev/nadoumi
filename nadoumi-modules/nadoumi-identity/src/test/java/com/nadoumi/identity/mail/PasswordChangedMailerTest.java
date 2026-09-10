package com.nadoumi.identity.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nadoumi.identity.service.mail.BrandProperties;
import com.nadoumi.identity.service.mail.EmailLayout;
import com.nadoumi.identity.service.mail.EmailMessage;
import com.nadoumi.identity.service.mail.MailSender;
import com.nadoumi.identity.service.mail.MailTemplates;
import com.nadoumi.identity.service.mail.PasswordChangedEvent;
import com.nadoumi.identity.service.mail.PasswordChangedMailer;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PasswordChangedMailerTest {

    private final MailSender mail = mock(MailSender.class);
    private final BrandProperties brand = new BrandProperties(
            "https://nadoumi.test", null, "Nadoumi",
            new BrandProperties.Contact(List.of("support@nadoumi.test"), List.of(), null, null),
            new BrandProperties.Social(null, null, null), null);
    private final PasswordChangedMailer mailer =
            new PasswordChangedMailer(new MailTemplates(), new EmailLayout(brand), mail, brand);

    @Test
    void sends_a_branded_security_notice_with_the_change_time_and_a_support_cta() {
        mailer.onPasswordChanged(new PasswordChangedEvent(42L, "user@nadoumi.test",
                Instant.parse("2026-02-03T09:15:00Z")));

        ArgumentCaptor<EmailMessage> sent = ArgumentCaptor.forClass(EmailMessage.class);
        verify(mail).send(sent.capture());
        EmailMessage msg = sent.getValue();
        assertThat(msg.to()).isEqualTo("user@nadoumi.test");
        assertThat(msg.subject()).isEqualTo("Your Nadoumi password was changed");
        assertThat(msg.htmlBody()).contains("3 Feb 2026, 09:15 UTC")   // HTML part is not column-wrapped
                .contains("was changed")
                .contains("mailto:support@nadoumi.test");
        assertThat(msg.body()).contains("09:15");
    }

    @Test
    void a_send_failure_is_swallowed_so_it_cannot_undo_the_password_change() {
        doThrow(new RuntimeException("smtp down")).when(mail).send(any());

        assertThatCode(() -> mailer.onPasswordChanged(
                new PasswordChangedEvent(42L, "user@nadoumi.test", Instant.now())))
                .doesNotThrowAnyException();
    }

    @Test
    void does_nothing_without_a_recipient_address() {
        mailer.onPasswordChanged(new PasswordChangedEvent(42L, null, Instant.now()));
        verify(mail, never()).send(any());
    }
}
