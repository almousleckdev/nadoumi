package com.nadoumi.identity.contact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nadoumi.common.outbox.OutboxEventTypes;
import com.nadoumi.common.outbox.OutboxWriter;
import com.nadoumi.identity.contact.mapper.ContactInquiryMapper;
import com.nadoumi.identity.web.request.ContactRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ContactServiceTest {

    private final ContactInquiryMapper mapper = mock(ContactInquiryMapper.class);
    private final OutboxWriter outboxWriter = mock(OutboxWriter.class);
    private final ContactService service = new ContactService(mapper, outboxWriter);

    ContactServiceTest() {
        doAnswer(inv -> {
            inv.getArgument(0, ContactInquiry.class).setId(77L);
            return null;
        }).when(mapper).insert(any(ContactInquiry.class));
    }

    private static ContactRequest req(String website) {
        return new ContactRequest("  Dana  ", "  Ali  ", "dana@example.com", " +212600000000 ",
                "PROGRAMMES", "  Programmes  ", "  Do you support September intake?  ", "en", website);
    }

    @Test
    void submit_persists_the_inquiry_and_emits_ContactInquiryReceived_to_the_outbox() {
        boolean accepted = service.submit(req(null), "203.0.113.7", "Mozilla/5.0");

        assertThat(accepted).isTrue();

        ArgumentCaptor<ContactInquiry> saved = ArgumentCaptor.forClass(ContactInquiry.class);
        verify(mapper).insert(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Dana Ali");
        assertThat(saved.getValue().getPhone()).isEqualTo("+212600000000");
        assertThat(saved.getValue().getCategory()).isEqualTo("PROGRAMMES");
        assertThat(saved.getValue().getStatus()).isEqualTo(ContactInquiryStatus.NEW);

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(outboxWriter).write(eq("contact_inquiry"), eq(77L),
                eq(OutboxEventTypes.CONTACT_INQUIRY_RECEIVED), payload.capture());
        assertThat(payload.getValue())
                .contains("\"inquiryId\":77")
                .contains("Dana Ali")
                .contains("PROGRAMMES");
    }

    @Test
    void a_filled_honeypot_is_dropped_silently_with_no_insert_and_no_event() {
        boolean accepted = service.submit(req("http://spam.example"), "203.0.113.7", "bot");

        assertThat(accepted).isFalse();
        verify(mapper, never()).insert(any());
        verify(outboxWriter, never()).write(anyString(), anyLong(), anyString(), anyString());
    }
}
