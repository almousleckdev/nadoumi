package com.nadoumi.identity.contact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nadoumi.identity.contact.mapper.ContactInquiryMapper;
import com.nadoumi.identity.service.mail.EmailMessage;
import com.nadoumi.identity.service.mail.MailSender;
import com.nadoumi.identity.service.mail.MailTemplates;
import com.nadoumi.identity.web.request.ContactRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ContactServiceTest {

    private final ContactInquiryMapper mapper = mock(ContactInquiryMapper.class);
    private final MailSender mail = mock(MailSender.class);
    private final ContactService service = new ContactService(
            mapper, mail, new MailTemplates(), "support@nadoumi.test");

    private static ContactRequest req(String website) {
        return new ContactRequest("  Dana Ali  ", "dana@example.com", "  Programmes  ",
                "  Do you support September intake?  ", "en", website);
    }

    @Test
    void submit_persists_the_inquiry_and_notifies_the_support_inbox() {
        boolean accepted = service.submit(req(null), "203.0.113.7", "Mozilla/5.0");

        assertThat(accepted).isTrue();

        ArgumentCaptor<ContactInquiry> saved = ArgumentCaptor.forClass(ContactInquiry.class);
        verify(mapper).insert(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Dana Ali");
        assertThat(saved.getValue().getSubject()).isEqualTo("Programmes");
        assertThat(saved.getValue().getMessage()).isEqualTo("Do you support September intake?");
        assertThat(saved.getValue().getIpAddress()).isEqualTo("203.0.113.7");
        assertThat(saved.getValue().getStatus()).isEqualTo(ContactInquiryStatus.NEW);

        ArgumentCaptor<EmailMessage> mailMsg = ArgumentCaptor.forClass(EmailMessage.class);
        verify(mail).send(mailMsg.capture());
        assertThat(mailMsg.getValue().to()).isEqualTo("support@nadoumi.test");
        assertThat(mailMsg.getValue().body()).contains("dana@example.com", "September intake");
    }

    @Test
    void a_filled_honeypot_is_dropped_silently_with_no_insert_and_no_mail() {
        boolean accepted = service.submit(req("http://spam.example"), "203.0.113.7", "bot");

        assertThat(accepted).isFalse();
        verify(mapper, never()).insert(any());
        verify(mail, never()).send(any());
    }

    @Test
    void a_mail_failure_does_not_fail_the_request_or_lose_the_inquiry() {
        doThrow(new RuntimeException("smtp down")).when(mail).send(any());

        boolean accepted = service.submit(req(null), "203.0.113.7", "Mozilla/5.0");

        assertThat(accepted).isTrue();
        verify(mapper).insert(any());
    }
}
