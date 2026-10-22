package com.nadoumi.payment.pdf;

import com.nadoumi.applicant.service.ApplicantDossier;
import com.nadoumi.applicant.web.response.ApplicantResponse;
import com.nadoumi.applicant.web.response.EducationResponse;
import com.nadoumi.applicant.web.response.FamilyMemberResponse;
import com.nadoumi.applicant.web.response.InterestResponse;
import com.nadoumi.applicant.web.response.PassportStatusResponse;
import com.nadoumi.applicant.web.response.PersonResponse;
import com.nadoumi.applicant.web.response.PersonalDetailsResponse;
import com.nadoumi.applicant.web.response.ResidenceResponse;
import com.nadoumi.applicant.web.response.TestScoreResponse;
import com.nadoumi.applicant.web.response.WorkResponse;
import com.nadoumi.application.service.ApplicationOpportunityService.Description;
import com.nadoumi.application.service.ApplicationOpportunityService.Kind;
import com.nadoumi.identity.service.mail.BrandProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Shared builders for the PDF tests. */
final class PdfFixtures {

    static final String ADDRESS = "Room 02, Floor 8, Unit 1, Building 6, Jiuzhou Beijun, Mianyang, Sichuan, China";

    private PdfFixtures() {
    }

    static PdfRenderer renderer() {
        BrandProperties brand = new BrandProperties("https://nadoumi.test", null, null, "Nadoumi",
                new BrandProperties.Contact(List.of("support@nadoumi.test"), List.of("+86 159 0823 7607"), ADDRESS, null),
                null, null);
        return new PdfRenderer(new PdfAssets(), brand);
    }

    static ReceiptModel receipt(String university) {
        return new ReceiptModel("NAD-RCT-2026-000007", Instant.parse("2026-10-22T06:30:00Z"), "Amina Diallo",
                "APL-1234", "amina@example.test", "APP-42", "MBBS", university, "AUTUMN_SEPTEMBER",
                "NAD-AB12CD34", "BANK1234567", Instant.parse("2026-10-22T06:29:00Z"),
                new BigDecimal("300"), "CNY", "Sichuan Nadoumi Education Consulting Co., Ltd.");
    }

    static ApplicationFormModel form(Description opportunity, String studentName) {
        ApplicantResponse profile = new ApplicantResponse(1L, "APL-1234", studentName, "Diallo", "2001-04-05",
                "Guinea", "P1234567", "amina@example.test", "+224600000000", "ACTIVE", "2026-01-01", "FEMALE",
                "Guinea", "Guinea", "French", "amina_wx", "+224600000000", true, true, false, null);
        PassportStatusResponse passport = new PassportStatusResponse("P1234567", studentName, "Diallo", "2001-04-05",
                "2022-01-10", "2032-01-09", "MANUAL", false, true, true, true, List.of());
        ApplicantDossier dossier = new ApplicantDossier(profile, passport,
                new ResidenceResponse(false, "Guinea", "Conakry", "12 Rue du Port", null, null, null, null),
                new InterestResponse("MASTER", List.of("Medicine"), List.of("Hangzhou"), "FULL", 2027, "AUTUMN", "English", null),
                List.of(new EducationResponse(1L, "Lycee de Conakry", "Guinea", "Conakry", "HIGH_SCHOOL", null, "Science",
                        new BigDecimal("3.4"), new BigDecimal("4"), LocalDate.of(2016, 9, 1), LocalDate.of(2019, 6, 30), false)),
                List.of(new WorkResponse(1L, "Clinique Sante", "Assistant", null, "Guinea", "Conakry",
                        LocalDate.of(2020, 1, 1), null, true, null, null, null)),
                List.of(new TestScoreResponse(1L, "IELTS", "6.5", null, LocalDate.of(2026, 3, 1), LocalDate.of(2028, 3, 1))),
                new PersonalDetailsResponse("SINGLE", "Muslim", "Conakry", "French", "12 Rue du Port, Conakry", "1001"),
                List.of(new FamilyMemberResponse(1L, "FATHER", "Ibrahima Diallo", LocalDate.of(1970, 2, 3), "Guinea",
                                false, null, "Port Authority", "Engineer", null, "+224611111111", null),
                        new FamilyMemberResponse(2L, "MOTHER", "Fatoumata Diallo", LocalDate.of(1975, 5, 6), "Guinea",
                                true, "Passed away in 2020", null, null, null, null, null)),
                new PersonResponse("SUPPORTER", "Ibrahima Diallo", "Father", "MALE", "ib@example.test", "+224611111111",
                        "Engineer", "Port Authority", "Quai 4, Conakry", "12 Rue du Port"),
                new PersonResponse("CHINA_EMERGENCY", "Li Wei", "Friend", "MALE", null, "+8613800000000",
                        null, "Shanghai Language School", null, "Shanghai"));
        return new ApplicationFormModel("NAD-APP-2026-000042", Instant.parse("2026-10-22T06:30:00Z"), "APP-42",
                opportunity, "NAD-AB12CD34", dossier, photo(), signature(), java.time.LocalDate.of(2026, 10, 22));
    }

    /** A small hand-drawn-looking PNG on white. */
    static byte[] signature() {
        try {
            var image = new java.awt.image.BufferedImage(400, 120, java.awt.image.BufferedImage.TYPE_INT_RGB);
            var g = image.createGraphics();
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, 400, 120);
            g.setColor(java.awt.Color.BLACK);
            g.setStroke(new java.awt.BasicStroke(4f));
            g.drawArc(20, 20, 160, 90, 30, 280);
            g.drawLine(180, 90, 380, 30);
            g.dispose();
            var out = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "png", out);
            return out.toByteArray();
        }
        catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    /** A small portrait-shaped JPEG. */
    static byte[] photo() {
        try {
            var image = new java.awt.image.BufferedImage(300, 400, java.awt.image.BufferedImage.TYPE_INT_RGB);
            var g = image.createGraphics();
            g.setColor(new java.awt.Color(0x94A3B8));
            g.fillRect(0, 0, 300, 400);
            g.setColor(new java.awt.Color(0x475569));
            g.fillOval(90, 80, 120, 140);
            g.fillRect(60, 240, 180, 160);
            g.dispose();
            var out = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "jpeg", out);
            return out.toByteArray();
        }
        catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    static Description programme() {
        return new Description(Kind.PROGRAM, "MBBS", "Fudan University", "AUTUMN_SEPTEMBER");
    }

    static Description scholarship() {
        return new Description(Kind.SCHOLARSHIP, "CSC Master Scholarship", null, null);
    }
}
