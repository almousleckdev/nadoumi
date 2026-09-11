package com.nadoumi.notification.outbox;

import com.nadoumi.common.notification.NotificationChannelKind;
import com.nadoumi.identity.service.mail.BrandProperties;
import com.nadoumi.identity.service.mail.EmailContent;
import com.nadoumi.identity.service.mail.EmailContent.ListItem;
import com.nadoumi.identity.service.mail.EmailLayout;
import com.nadoumi.identity.service.mail.EmailMessage;
import com.nadoumi.identity.service.mail.EmailRender;
import com.nadoumi.identity.service.mail.MailSender;
import com.nadoumi.notification.domain.NotificationType;
import com.nadoumi.notification.domain.OutboxEvent;
import com.nadoumi.notification.render.NotificationRenderer;
import com.nadoumi.notification.service.NotificationRequest;
import com.nadoumi.notification.service.NotificationService;
import com.nadoumi.program.service.ProgramService;
import com.nadoumi.scholarship.mapper.ScholarshipSearch;
import com.nadoumi.scholarship.service.ScholarshipService;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds and sends the "Welcome to Nadoumi" email for a {@code StudentRegistered}
 * outbox event. It owns the whole welcome flow because the email carries
 * structured programme / scholarship sections the generic flat-string dispatch
 * path does not model:
 *
 * <ol>
 *   <li>create the {@code WELCOME} notification — {@code IN_APP} only, idempotent
 *       on {@code (userId, source_ref)};</li>
 *   <li>pull a few current published programmes and soon-closing scholarships from
 *       the public catalog services (each section is dropped when empty — no
 *       placeholders);</li>
 *   <li>render through the shared {@link EmailLayout} and send via {@link MailSender},
 *       then record the EMAIL delivery so it shows in the staff console.</li>
 * </ol>
 */
public class WelcomeContentComposer {

    private static final Logger log = LoggerFactory.getLogger(WelcomeContentComposer.class);

    private static final String LOCALE = NotificationRenderer.DEFAULT_LOCALE;
    private static final int SECTION_SIZE = 3;

    private final NotificationService notificationService;
    private final NotificationRenderer renderer;
    private final EmailLayout emailLayout;
    private final MailSender mailSender;
    private final BrandProperties brand;
    private final ScholarshipService scholarshipService;
    private final ProgramService programService;

    public WelcomeContentComposer(NotificationService notificationService, NotificationRenderer renderer,
            EmailLayout emailLayout, MailSender mailSender, BrandProperties brand,
            ScholarshipService scholarshipService, ProgramService programService) {
        this.notificationService = notificationService;
        this.renderer = renderer;
        this.emailLayout = emailLayout;
        this.mailSender = mailSender;
        this.brand = brand;
        this.scholarshipService = scholarshipService;
        this.programService = programService;
    }

    public void handle(OutboxEvent event, Map<String, Object> context) {
        Long userId = asLong(context.get("userId"));
        if (userId == null) {
            log.warn("StudentRegistered outbox event id={} has no userId — skipped", event.getId());
            return;
        }
        String sourceRef = "outbox:" + event.getId() + ":" + userId;

        String inAppBody = renderer.render(NotificationType.WELCOME, NotificationChannelKind.IN_APP, LOCALE, context)
                .body();
        long notificationId = notificationService.create(NotificationRequest.fromEvent(
                userId, NotificationType.WELCOME, NotificationType.WELCOME.defaultTitle(),
                inAppBody, sourceRef, event.getPayloadJson()));
        if (notificationId == 0L) {
            return; // already handled on an earlier delivery — the email went out then
        }

        String recipient = str(context.get("email"));
        if (recipient == null || recipient.isBlank()) {
            log.warn("StudentRegistered outbox event id={} has no email — in-app only", event.getId());
            notificationService.recordDirectEmailDelivery(notificationId, false, "no recipient address");
            return;
        }

        boolean sent = true;
        String error = null;
        try {
            mailSender.send(compose(recipient, str(context.get("firstName")), context));
        } catch (RuntimeException e) {
            sent = false;
            error = e.toString();
            log.warn("welcome email send failed for userId={}", userId, e);
        }
        notificationService.recordDirectEmailDelivery(notificationId, sent, error);
    }

    private EmailMessage compose(String recipient, String firstName, Map<String, Object> context) {
        NotificationRenderer.Rendered tpl =
                renderer.render(NotificationType.WELCOME, NotificationChannelKind.EMAIL, LOCALE, context);
        String heading = firstName == null || firstName.isBlank()
                ? "Welcome to Nadoumi"
                : "Welcome to Nadoumi, " + firstName;
        EmailContent content = EmailContent.builder(heading)
                .preheader("Your Nadoumi account is ready")
                .paragraphs(tpl.body())
                .itemGroup("Programmes to explore", programItems())
                .itemGroup("Scholarships closing soon", scholarshipItems())
                .cta("Explore Nadoumi", brand.url("/"))
                .showPreferencesLink(false)
                .build();
        EmailRender r = emailLayout.render(content);
        String subject = tpl.subject() == null || tpl.subject().isBlank() ? "Welcome to Nadoumi" : tpl.subject();
        return new EmailMessage(recipient, subject, r.text(), r.html());
    }

    /** Most recently published programmes. Any failure degrades to an empty section. */
    private List<ListItem> programItems() {
        try {
            return programService
                    .publicList(null, null, null, null, null, null, null, 0, SECTION_SIZE)
                    .content().stream()
                    .map(p -> new ListItem(
                            join(p.name(), p.universityName(), " — "),
                            join(p.programType(), p.field(), " · "),
                            brand.url("/programs/" + p.slug())))
                    .toList();
        } catch (RuntimeException e) {
            log.warn("welcome email: could not load featured programmes", e);
            return List.of();
        }
    }

    /** Published + active scholarships with the soonest deadline. */
    private List<ListItem> scholarshipItems() {
        try {
            ScholarshipSearch bySoonestDeadline = new ScholarshipSearch(
                    null, null, null, null, null, null, null, null, null, null, null, null,
                    null, null, null, "deadline", null, null);
            return scholarshipService.list(bySoonestDeadline, 0, SECTION_SIZE)
                    .content().stream()
                    .map(s -> new ListItem(
                            s.title(),
                            scholarshipMeta(s.deadline(), s.country()),
                            brand.url("/scholarships/" + s.slug())))
                    .toList();
        } catch (RuntimeException e) {
            log.warn("welcome email: could not load scholarships closing soon", e);
            return List.of();
        }
    }

    private static String scholarshipMeta(Object deadline, String country) {
        String left = deadline == null ? null : "Deadline " + deadline;
        return join(left, country, " · ");
    }

    private static String join(String a, String b, String sep) {
        boolean hasA = a != null && !a.isBlank();
        boolean hasB = b != null && !b.isBlank();
        if (hasA && hasB) {
            return a + sep + b;
        }
        return hasA ? a : (hasB ? b : "");
    }

    private static Long asLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return value == null ? null : Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
