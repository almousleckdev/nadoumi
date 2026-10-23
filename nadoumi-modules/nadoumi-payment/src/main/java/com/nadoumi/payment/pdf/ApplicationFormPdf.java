package com.nadoumi.payment.pdf;

import com.nadoumi.applicant.service.ApplicantDossier;
import com.nadoumi.applicant.web.response.ApplicantResponse;
import com.nadoumi.applicant.web.response.EducationResponse;
import com.nadoumi.applicant.web.response.InterestResponse;
import com.nadoumi.applicant.web.response.PassportStatusResponse;
import com.nadoumi.applicant.web.response.PersonResponse;
import com.nadoumi.applicant.web.response.PersonalDetailsResponse;
import com.nadoumi.applicant.web.response.ResidenceResponse;
import com.nadoumi.applicant.web.response.TestScoreResponse;
import com.nadoumi.applicant.web.response.WorkResponse;
import com.nadoumi.application.service.ApplicationOpportunityService.Kind;
import com.nadoumi.payment.pdf.PdfRenderer.Page;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * The Nadoumi application form as a PDF, built from the applicant's profile. A section whose data the portal
 * does not collect yet is printed as "Not provided" rather than invented.
 */
@Component
public class ApplicationFormPdf {

    public static final String TITLE = "Application Form";

    private static final List<String> AFFIRMATION = List.of(
            "The information I have provided is true, complete and accurate.",
            "I authorise Nadoumi to apply on my behalf and to receive my admission documents, including the JW202.",
            "I will obey the laws of China during my stay.",
            "I will obey the arrangements and rules of the host university.",
            "I accept sanctions if I violate the law or the rules, and I understand that a false statement can "
                    + "cancel my application or admission.");

    private final PdfRenderer renderer;

    public ApplicationFormPdf(PdfRenderer renderer) {
        this.renderer = renderer;
    }

    public byte[] render(ApplicationFormModel m) {
        ApplicantDossier d = m.dossier();
        return renderer.render(TITLE + " " + m.formNo(), m.formNo(), m.verifyUrl(), page -> {
            page.masthead(TITLE, List.of(
                    new String[] {"Form no.", m.formNo()},
                    new String[] {"Application", m.applicationRef()},
                    new String[] {"Generated", PdfText.stamp(m.generatedAt())},
                    new String[] {"Application fee", "PAID  " + m.paymentReference()}), m.photo());
            opportunity(page, m);
            personal(page, d);
            tests(page, d.testScores());
            education(page, d.education());
            work(page, d.work());
            family(page, d);
            studyPlan(page, d.interests());
            page.section(8, "Applicant affirmation");
            for (int i = 0; i < AFFIRMATION.size(); i++) {
                page.paragraph((i + 1) + ".  " + AFFIRMATION.get(i));
            }
            page.note("The application is not valid without the applicant's signature.");
            page.signatureLine("Applicant's signature", m.signature(), m.signedOn() == null ? null : m.signedOn().toString());
        });
    }

    private void opportunity(Page page, ApplicationFormModel m) {
        page.section(1, "Applying for");
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {m.opportunity().kind() == Kind.PROGRAM
                ? "Programme" : "Scholarship", m.opportunity().title()});
        if (m.opportunity().universityName() != null) {
            rows.add(new String[] {"University", m.opportunity().universityName()});
        }
        if (m.opportunity().intake() != null) {
            rows.add(new String[] {"Intake", PdfText.label(m.opportunity().intake())});
        }
        page.pairs(rows);
    }

    private void personal(Page page, ApplicantDossier d) {
        ApplicantResponse p = d.profile();
        PassportStatusResponse pass = d.passport();
        ResidenceResponse r = d.residence();
        page.section(2, "Personal information");
        List<String[]> rows = new ArrayList<>(List.of(
                new String[] {"Surname", firstNonBlank(pass.familyName(), p.familyName())},
                new String[] {"Given name", firstNonBlank(pass.givenName(), p.givenName())},
                new String[] {"Date of birth", firstNonBlank(pass.dob(), p.dob())},
                new String[] {"Gender", PdfText.label(p.gender())},
                new String[] {"Nationality", p.nationality()},
                new String[] {"Country of origin", p.countryOfOrigin()},
                new String[] {"Native language", p.nativeLanguage()},
                new String[] {"Passport no.", firstNonBlank(pass.passportNo(), p.passportNo())},
                new String[] {"Passport issued", pass.issueDate()},
                new String[] {"Passport expires", pass.expiryDate()},
                new String[] {"Email", p.email()},
                new String[] {"Phone", p.phone()},
                new String[] {"WeChat", p.wechatId()},
                new String[] {"WhatsApp", p.whatsapp()},
                new String[] {"Country of residence", p.countryOfResidence()}));
        PersonalDetailsResponse extra = d.personal();
        if (extra != null) {
            rows.add(new String[] {"Marital status", PdfText.label(extra.maritalStatus())});
            rows.add(new String[] {"Religion", extra.religion()});
            rows.add(new String[] {"Place of birth", extra.placeOfBirth()});
            rows.add(new String[] {"Official language", extra.officialLanguage()});
            rows.add(new String[] {"Permanent address",
                    joinNonBlank(", ", extra.permanentAddress(), extra.permanentPostcode())});
        }
        if (r != null) {
            rows.add(new String[] {"Currently in China", r.inChina() ? "Yes" : "No"});
            rows.add(new String[] {"Current address", joinNonBlank(", ", r.address(), r.city(), r.country())});
        }
        page.pairs(rows);
    }

    private void tests(Page page, List<TestScoreResponse> scores) {
        page.section(3, "Language and test scores");
        if (scores.isEmpty()) {
            page.note("Not provided.");
            return;
        }
        page.table(List.of("Test", "Score", "Taken", "Expires"),
                scores.stream().map(s -> List.of(Objects.toString(s.testType(), ""), Objects.toString(s.score(), ""),
                        Objects.toString(s.takenOn(), ""), Objects.toString(s.expiresOn(), ""))).toList(),
                new float[] {3f, 2f, 2f, 2f});
    }

    private void education(Page page, List<EducationResponse> education) {
        page.section(4, "Education background");
        if (education.isEmpty()) {
            page.note("Not provided.");
            return;
        }
        page.table(List.of("Level", "Institution", "Country", "Field", "Period", "GPA"),
                education.stream().map(e -> List.of(
                        Objects.toString(PdfText.label(e.level()), ""), Objects.toString(e.institution(), ""),
                        Objects.toString(e.country(), ""), Objects.toString(e.field(), ""),
                        period(e.startDate(), e.endDate(), e.current()),
                        e.gpa() == null ? "" : e.gpa() + (e.gpaScale() == null ? "" : " / " + e.gpaScale()))).toList(),
                new float[] {1.6f, 3f, 1.6f, 2.2f, 2.4f, 1.2f});
    }

    private void work(Page page, List<WorkResponse> work) {
        page.section(5, "Work experience");
        if (work.isEmpty()) {
            page.note("Not provided.");
            return;
        }
        page.table(List.of("Employer", "Position", "Location", "Period"),
                work.stream().map(w -> List.of(
                        Objects.toString(w.employer(), ""), Objects.toString(w.jobTitle(), ""),
                        joinNonBlank(", ", w.city(), w.country()), period(w.startDate(), w.endDate(), w.current()))).toList(),
                new float[] {3f, 2.4f, 2.4f, 2.4f});
    }

    private void family(Page page, ApplicantDossier d) {
        page.section(6, "Family, financial supporter and emergency contact");
        page.subheading("Family members");
        if (d.family().isEmpty()) {
            page.note("Not provided.");
        }
        else {
            page.table(List.of("Relation", "Name", "Born", "Nationality", "Employer and position", "Contact"),
                    d.family().stream().map(f -> List.of(
                            PdfText.label(f.relation()) + (f.deceased() ? " (deceased)" : ""),
                            Objects.toString(f.fullName(), ""), Objects.toString(f.dateOfBirth(), ""),
                            Objects.toString(f.nationality(), ""),
                            joined(f.company(), f.position(), f.deceased() ? f.note() : null),
                            joined(f.phone(), f.email()))).toList(),
                    new float[] {1.5f, 2.2f, 1.6f, 1.6f, 2.8f, 2.4f});
        }
        person(page, "Financial supporter", d.supporter());
        person(page, "Emergency contact in China", d.chinaEmergency());
    }

    private void person(Page page, String title, PersonResponse p) {
        page.subheading(title);
        if (p == null) {
            page.note("Not provided.");
            return;
        }
        page.pairs(List.of(
                new String[] {"Name", p.fullName()},
                new String[] {"Relationship", p.relationship()},
                new String[] {"Gender", PdfText.label(p.gender())},
                new String[] {"Phone", p.phone()},
                new String[] {"Email", p.email()},
                new String[] {"Position", p.position()},
                new String[] {"Company", p.company()},
                new String[] {"Company address", p.companyAddress()},
                new String[] {"Home address", p.homeAddress()}));
    }

    private void studyPlan(Page page, InterestResponse i) {
        page.section(7, "Study plan");
        if (i == null) {
            page.note("Not provided.");
            return;
        }
        page.pairs(List.of(
                new String[] {"Desired level", PdfText.label(i.desiredLevel())},
                new String[] {"Fields of study", joinList(i.fields())},
                new String[] {"Preferred cities", joinList(i.cities())},
                new String[] {"Teaching language", i.teachingLanguage()},
                new String[] {"Scholarship", PdfText.label(i.scholarshipInterest())},
                new String[] {"Intake", joinNonBlank(" ", PdfText.label(i.intakeTerm()), Objects.toString(i.intakeYear(), ""))},
                new String[] {"Notes", i.notes()}));
    }

    private static String period(LocalDate from, LocalDate to, boolean current) {
        String start = from == null ? "" : from.toString();
        String end = current ? "present" : to == null ? "" : to.toString();
        return start.isEmpty() && end.isEmpty() ? "" : start + " to " + end;
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private static String joinList(List<String> values) {
        return values == null || values.isEmpty() ? null : String.join(", ", values);
    }

    /** Comma-joined non-blank parts, or an empty string when there are none (a table cell). */
    private static String joined(String... parts) {
        String joined = joinNonBlank(", ", parts);
        return joined == null ? "" : joined;
    }

    private static String joinNonBlank(String separator, String... parts) {
        String joined = java.util.Arrays.stream(parts).filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(separator));
        return joined.isEmpty() ? null : joined;
    }
}
