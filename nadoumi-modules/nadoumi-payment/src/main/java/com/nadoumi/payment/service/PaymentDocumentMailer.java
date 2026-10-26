package com.nadoumi.payment.service;

import com.nadoumi.applicant.service.ApplicantDossier;
import com.nadoumi.application.service.ApplicationOpportunityService.Description;
import com.nadoumi.identity.service.mail.BrandProperties;
import com.nadoumi.identity.service.mail.EmailAttachment;
import com.nadoumi.identity.service.mail.EmailContent;
import com.nadoumi.identity.service.mail.EmailContent.KeyValues.Entry;
import com.nadoumi.identity.service.mail.EmailLayout;
import com.nadoumi.identity.service.mail.EmailMessage;
import com.nadoumi.identity.service.mail.EmailRender;
import com.nadoumi.payment.domain.ApplicationPayment;
import com.nadoumi.payment.domain.PaymentDocument;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Builds the two emails: the receipt to the student, and the submitted application form to an administrator.
 * Header fields are cleaned of line breaks and control characters (a student's name goes into a subject).
 */
@Component
public class PaymentDocumentMailer {

    private static final String PDF = "application/pdf";
    private static final int SUBJECT_MAX = 150;

    private final EmailLayout layout;
    private final BrandProperties brand;

    public PaymentDocumentMailer(EmailLayout layout, BrandProperties brand) {
        this.layout = layout;
        this.brand = brand;
    }

    public EmailMessage receipt(String to, String firstName, ApplicationPayment payment, PaymentDocument receipt,
            byte[] pdf) {
        String heading = firstName == null || firstName.isBlank() ? "Payment received" : "Payment received, " + firstName;
        boolean serviceFee = com.nadoumi.application.service.PaymentGuardProvider.SERVICE_FEE.equals(payment.getKind());
        EmailContent content = EmailContent.builder(heading)
                .preheader(serviceFee ? "Your Nadoumi service fee receipt" : "Your Nadoumi application fee receipt")
                .paragraphs(serviceFee
                        ? "Thank you. We confirmed your Nadoumi service fee. Your receipt is attached to this email.\n\n"
                                + "Your admission letter and JW202 form are now available in your dashboard."
                        : "Thank you. We confirmed your Nadoumi application fee. Your receipt is attached to this email.\n\n"
                                + "Next, complete your documents and submit your application.")
                .keyValues(List.of(
                        new Entry("Receipt no.", receipt.getDocNumber()),
                        new Entry("Application", PaymentDocumentService.applicationRef(payment)),
                        new Entry("Amount", payment.getCurrency() + " "
                                + payment.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString()),
                        new Entry("Payment reference", payment.getReference())))
                .cta("Open my application", brand.url("/dashboard/applications/" + payment.getApplicationId()))
                .showPreferencesLink(false)
                .build();
        EmailRender rendered = layout.render(content);
        return new EmailMessage(to, header("Your Nadoumi payment receipt " + receipt.getDocNumber()),
                rendered.text(), rendered.html())
                .withAttachment(new EmailAttachment(receipt.getDocNumber() + ".pdf", PDF, pdf));
    }

    public EmailMessage submittedForm(String to, ApplicationPayment payment, PaymentDocument form, ApplicantDossier dossier,
            Description opportunity, byte[] pdf) {
        String student = (nullToEmpty(dossier.profile().givenName()) + " " + nullToEmpty(dossier.profile().familyName())).strip();
        List<Entry> entries = new ArrayList<>(List.of(
                new Entry("Application", PaymentDocumentService.applicationRef(payment)),
                new Entry("Student", student),
                new Entry("Nationality", nullToEmpty(dossier.profile().nationality())),
                new Entry("Applying for", opportunity.title())));
        if (opportunity.universityName() != null) {
            entries.add(new Entry("University", opportunity.universityName()));
        }
        entries.add(new Entry("Payment reference", payment.getReference()));
        EmailContent content = EmailContent.builder("New application submitted")
                .preheader("A student submitted an application")
                .paragraphs("A student submitted an application and paid the application fee. The application form is attached.")
                .keyValues(entries)
                .showPreferencesLink(false)
                .build();
        EmailRender rendered = layout.render(content);
        String subject = "[Nadoumi] New application " + PaymentDocumentService.applicationRef(payment) + ": " + student
                + ", " + opportunity.title();
        return new EmailMessage(to, header(subject), rendered.text(), rendered.html())
                .withAttachment(new EmailAttachment(form.getDocNumber() + ".pdf", PDF, pdf));
    }

    /** One line, no control characters, bounded: safe as an email header value. */
    static String header(String value) {
        String clean = value.replaceAll("[\\p{Cntrl}\\u2028\\u2029]+", " ").replaceAll("\\s{2,}", " ").strip();
        return clean.length() <= SUBJECT_MAX ? clean : clean.substring(0, SUBJECT_MAX).strip();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
