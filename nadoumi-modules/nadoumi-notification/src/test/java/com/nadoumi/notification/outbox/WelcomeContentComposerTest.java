package com.nadoumi.notification.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.common.notification.NotificationChannelKind;
import com.nadoumi.common.web.PageResponse;
import com.nadoumi.identity.service.mail.BrandProperties;
import com.nadoumi.identity.service.mail.EmailLayout;
import com.nadoumi.identity.service.mail.EmailMessage;
import com.nadoumi.identity.service.mail.MailSender;
import com.nadoumi.notification.domain.NotificationType;
import com.nadoumi.notification.domain.OutboxEvent;
import com.nadoumi.notification.render.NotificationRenderer;
import com.nadoumi.notification.service.NotificationRequest;
import com.nadoumi.notification.service.NotificationService;
import com.nadoumi.program.service.ProgramService;
import com.nadoumi.program.web.response.PublicProgramResponse;
import com.nadoumi.scholarship.mapper.ScholarshipSearch;
import com.nadoumi.scholarship.service.ScholarshipService;
import com.nadoumi.scholarship.web.response.PublicScholarshipResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class WelcomeContentComposerTest {

    private final NotificationService notificationService = mock(NotificationService.class);
    private final NotificationRenderer renderer = mock(NotificationRenderer.class);
    private final MailSender mailSender = mock(MailSender.class);
    private final ScholarshipService scholarshipService = mock(ScholarshipService.class);
    private final ProgramService programService = mock(ProgramService.class);

    private final BrandProperties brand = new BrandProperties(
            "https://nadoumi.test", null, "Nadoumi",
            new BrandProperties.Contact(List.of("support@nadoumi.test"), List.of(), null, null),
            new BrandProperties.Social(null, null, null), null);

    private final WelcomeContentComposer composer = new WelcomeContentComposer(
            notificationService, renderer, new EmailLayout(brand), mailSender, brand,
            scholarshipService, programService);

    private static OutboxEvent event(String payload) {
        OutboxEvent e = new OutboxEvent();
        e.setId(9L);
        e.setType("StudentRegistered");
        e.setPayloadJson(payload);
        return e;
    }

    private static Map<String, Object> ctx(Object userId) {
        return Map.of("userId", userId, "email", "amina@example.test", "firstName", "Amina", "locale", "en");
    }

    @BeforeEach
    void stubTemplates() {
        when(renderer.render(eq(NotificationType.WELCOME), eq(NotificationChannelKind.IN_APP), any(), any()))
                .thenReturn(new NotificationRenderer.Rendered(null, "Welcome to Nadoumi - explore from your dashboard."));
        when(renderer.render(eq(NotificationType.WELCOME), eq(NotificationChannelKind.EMAIL), any(), any()))
                .thenReturn(new NotificationRenderer.Rendered("Welcome to Nadoumi",
                        "Your Nadoumi account is ready."));
        when(notificationService.create(any())).thenReturn(500L);
    }

    // Real DTOs (records can't be Mockito-mocked here — final accessors). Only the
    // fields the composer reads carry meaningful values; the rest are null/false.
    private static PublicProgramResponse program(String name, String uni, String slug) {
        return new PublicProgramResponse(
                1L, 2L, uni, "uni-slug", name, slug, null, "Bachelor", List.of(), "Medicine",
                null, null, null, null, null, null, null, null, null, false, false, null, null);
    }

    private static PublicScholarshipResponse scholarship(String title, String country, String slug,
            LocalDate deadline) {
        return new PublicScholarshipResponse(
                1L, slug, "REF-1", title, null, country, null, null, null, null, null, false,
                null, null, null, null, false, false, deadline, null, null, null, false, false, false,
                null, null, null, null, null, null, List.of(), List.of(), List.of(),
                null, null, null, null, null, null, null, null, null, null);
    }

    private EmailMessage captureSentMail() {
        ArgumentCaptor<EmailMessage> sent = ArgumentCaptor.forClass(EmailMessage.class);
        verify(mailSender).send(sent.capture());
        return sent.getValue();
    }

    @Test
    void sends_a_personalised_welcome_with_both_sections_when_catalog_data_exists() {
        when(programService.publicList(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(PageResponse.of(List.of(program("MBBS", "Fudan University", "mbbs-fudan")), 0, 3, 1));
        when(scholarshipService.list(any(ScholarshipSearch.class), anyInt(), anyInt()))
                .thenReturn(PageResponse.of(
                        List.of(scholarship("CSC Type A", "China", "csc-a", LocalDate.of(2026, 6, 30))), 0, 3, 1));

        composer.handle(event("{}"), ctx(77));

        EmailMessage msg = captureSentMail();
        assertThat(msg.to()).isEqualTo("amina@example.test");
        assertThat(msg.subject()).isEqualTo("Welcome to Nadoumi");
        assertThat(msg.htmlBody())
                .contains("Welcome to Nadoumi, Amina")
                .contains("Programmes to explore").contains("MBBS").contains("Fudan University")
                .contains("https://nadoumi.test/programs/mbbs-fudan")
                .contains("Scholarships closing soon").contains("CSC Type A")
                .contains("https://nadoumi.test/scholarships/csc-a")
                .contains("https://nadoumi.test/");                                // explore CTA
        verify(notificationService).recordDirectEmailDelivery(500L, true, null);
    }

    @Test
    void drops_a_section_when_its_catalog_query_is_empty_and_never_invents_rows() {
        when(programService.publicList(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(PageResponse.of(List.of(), 0, 3, 0));
        when(scholarshipService.list(any(ScholarshipSearch.class), anyInt(), anyInt()))
                .thenReturn(PageResponse.of(
                        List.of(scholarship("CSC Type A", "China", "csc-a", LocalDate.of(2026, 6, 30))), 0, 3, 1));

        composer.handle(event("{}"), ctx(77));

        String html = captureSentMail().htmlBody();
        assertThat(html).doesNotContain("Programmes to explore");
        assertThat(html).contains("Scholarships closing soon").contains("CSC Type A");
    }

    @Test
    void targets_only_the_registering_user() {
        stubEmptyCatalog();

        composer.handle(event("{}"), ctx(77));

        ArgumentCaptor<NotificationRequest> req = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).create(req.capture());
        assertThat(req.getValue().recipientUserId()).isEqualTo(77L);
        assertThat(req.getValue().type()).isEqualTo(NotificationType.WELCOME);
        assertThat(req.getValue().sourceRef()).isEqualTo("outbox:9:77");
    }

    @Test
    void is_idempotent_when_the_notification_already_exists() {
        when(notificationService.create(any())).thenReturn(0L);

        composer.handle(event("{}"), ctx(77));

        verify(mailSender, never()).send(any());
        verify(notificationService, never()).recordDirectEmailDelivery(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyBoolean(), any());
    }

    @Test
    void records_a_failed_delivery_when_the_send_throws() {
        stubEmptyCatalog();
        doThrow(new RuntimeException("smtp down")).when(mailSender).send(any());

        composer.handle(event("{}"), ctx(77));

        verify(notificationService).recordDirectEmailDelivery(eq(500L), eq(false), any());
    }

    @Test
    void queries_scholarships_ordered_by_soonest_deadline() {
        stubEmptyCatalog();

        composer.handle(event("{}"), ctx(77));

        ArgumentCaptor<ScholarshipSearch> search = ArgumentCaptor.forClass(ScholarshipSearch.class);
        verify(scholarshipService).list(search.capture(), anyInt(), anyInt());
        assertThat(search.getValue().sort()).isEqualTo("deadline");
    }

    private void stubEmptyCatalog() {
        when(programService.publicList(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(PageResponse.of(List.of(), 0, 3, 0));
        when(scholarshipService.list(any(ScholarshipSearch.class), anyInt(), anyInt()))
                .thenReturn(PageResponse.of(List.of(), 0, 3, 0));
    }
}
