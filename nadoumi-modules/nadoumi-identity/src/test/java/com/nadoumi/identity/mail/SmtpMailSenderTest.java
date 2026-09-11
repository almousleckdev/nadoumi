package com.nadoumi.identity.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.identity.service.mail.EmailMessage;
import com.nadoumi.identity.service.mail.SmtpMailSender;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

class SmtpMailSenderTest {

    private final JavaMailSender javaMail = mock(JavaMailSender.class);
    private final SmtpMailSender sender = new SmtpMailSender(javaMail, "no-reply@nadoumi.test");

    @Test
    void text_only_message_takes_the_simple_path() {
        sender.send(EmailMessage.text("s@x.test", "Plain", "just text"));

        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMail).send(sent.capture());
        assertThat(sent.getValue().getText()).isEqualTo("just text");
        verify(javaMail, never()).send(any(MimeMessage.class));
    }

    @Test
    void html_message_is_sent_as_multipart_with_both_a_text_and_an_html_part() throws Exception {
        when(javaMail.createMimeMessage()).thenReturn(new JavaMailSenderImpl().createMimeMessage());

        sender.send(new EmailMessage("s@x.test", "Rich", "plain fallback", "<p>rich</p>"));

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMail).send(sent.capture());
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        sent.getValue().writeTo(raw); // implies saveChanges() — fills in the MIME headers
        String mime = raw.toString();
        assertThat(mime).contains("multipart/");
        assertThat(mime).contains("text/plain").contains("text/html");
        assertThat(mime).contains("plain fallback").contains("<p>rich</p>");
        verify(javaMail, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void a_transport_failure_surfaces_as_a_MailException() {
        doThrow(new MailSendException("smtp down")).when(javaMail).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> sender.send(EmailMessage.text("s@x.test", "x", "y")))
                .isInstanceOf(MailException.class);
    }
}
