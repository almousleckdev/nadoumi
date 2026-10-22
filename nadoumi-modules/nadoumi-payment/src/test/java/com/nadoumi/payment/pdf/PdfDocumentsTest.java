package com.nadoumi.payment.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class PdfDocumentsTest {

    private final PdfRenderer renderer = PdfFixtures.renderer();
    private final ReceiptPdf receipts = new ReceiptPdf(renderer);
    private final ApplicationFormPdf forms = new ApplicationFormPdf(renderer);

    private static String text(byte[] pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    @Test
    void shouldPrintTheReceiptFacts_andTheCompanyFooter() throws IOException {
        byte[] pdf = receipts.render(PdfFixtures.receipt("Fudan University"));

        String text = text(pdf);
        assertThat(text).contains("Payment Receipt", "NAD-RCT-2026-000007", "Amina Diallo", "NAD-AB12CD34",
                "BANK1234567", "¥ 300.00", "PAID", "Fudan University", "non-refundable");
        assertThat(text).contains(PdfFixtures.ADDRESS, "support@nadoumi.test", "+86 159 0823 7607", "Page 1 / 1");
        assertThat(pdf).startsWith("%PDF".getBytes());
    }

    @Test
    void shouldLeaveTheUniversityOut_whenTheDescriptionHasNone() throws IOException {
        String receipt = text(receipts.render(PdfFixtures.receipt(null)));
        String form = text(forms.render(PdfFixtures.form(PdfFixtures.scholarship(), "Amina")));

        assertThat(receipt).doesNotContain("University").doesNotContain("Fudan");
        assertThat(form).contains("CSC Master Scholarship").doesNotContain("Fudan").doesNotContain("University:");
    }

    @Test
    void shouldNameTheUniversity_forAProgrammeApplication() throws IOException {
        String form = text(forms.render(PdfFixtures.form(PdfFixtures.programme(), "Amina")));

        assertThat(form).contains("Fudan University", "MBBS", "Autumn september");
    }

    @Test
    void shouldPrintTheProfileSections_andMarkWhatIsNotCollected() throws IOException {
        String form = text(forms.render(PdfFixtures.form(PdfFixtures.programme(), "Amina")));

        assertThat(form).contains("Personal information", "P1234567", "2032-01-09", "IELTS", "6.5",
                "High school", "Lycee de Conakry", "Clinique Sante", "present", "Medicine", "Hangzhou",
                "Marital status", "Single", "Muslim", "Place of birth", "Ibrahima Diallo", "Fatoumata Diallo", "(deceased)",
                "Passed away", "Financial supporter", "Port Authority", "Emergency contact in China", "Li Wei",
                "Applicant affirmation",
                "Applicant's signature", "NAD-APP-2026-000042", "PAID");
    }

    @Test
    void shouldDrawHostileOrUnsupportedTextAsPlainText_withoutFailing() throws IOException {
        String hostile = "<img src=\"http://127.0.0.1:1/x\"> @import url(file:///etc/passwd) \u0001 中文";

        byte[] pdf = forms.render(PdfFixtures.form(PdfFixtures.programme(), hostile));

        String text = text(pdf);
        assertThat(text).contains("<img src=\"http://127.0.0.1:1/x\">").doesNotContain("\u0001");
        assertThat(text).contains("??"); // glyphs the font lacks become '?', never a failure
    }

    @Test
    void shouldRepeatTheFooterOnEveryPage_ofALongForm() throws IOException {
        var model = PdfFixtures.form(PdfFixtures.programme(), "Amina");
        var many = new java.util.ArrayList<com.nadoumi.applicant.web.response.EducationResponse>();
        for (int i = 0; i < 60; i++) {
            many.add(model.dossier().education().get(0));
        }
        var dossier = new com.nadoumi.applicant.service.ApplicantDossier(model.dossier().profile(),
                model.dossier().passport(), model.dossier().residence(), model.dossier().interests(), many,
                model.dossier().work(), model.dossier().testScores(), model.dossier().personal(), model.dossier().family(),
                model.dossier().supporter(), model.dossier().chinaEmergency());

        byte[] pdf = forms.render(new ApplicationFormModel(model.formNo(), model.generatedAt(), model.applicationRef(),
                model.opportunity(), model.paymentReference(), dossier));

        try (PDDocument doc = Loader.loadPDF(pdf)) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(1);
            for (int page = 1; page <= doc.getNumberOfPages(); page++) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                assertThat(stripper.getText(doc)).contains(PdfFixtures.ADDRESS, "Page " + page + " / " + doc.getNumberOfPages());
            }
        }
    }

    @Test
    void shouldKeepTheWatermarkFaint_soTextStaysLegible() {
        assertThat(PdfRenderer.WATERMARK_OPACITY).isLessThanOrEqualTo(0.06f);
    }

    /** Writes both documents as PNG previews for a human to look at. Opt-in: -Dpdf.preview=<dir>. */
    @Test
    void shouldWritePreviews_whenAskedTo() throws IOException {
        String dir = System.getProperty("pdf.preview");
        if (dir == null) {
            return;
        }
        Files.createDirectories(Path.of(dir));
        save(receipts.render(PdfFixtures.receipt("Fudan University")), Path.of(dir, "receipt"));
        save(forms.render(PdfFixtures.form(PdfFixtures.programme(), "Amina")), Path.of(dir, "form"));
    }

    private static void save(byte[] pdf, Path base) throws IOException {
        Files.write(Path.of(base + ".pdf"), pdf);
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            PDFRenderer r = new PDFRenderer(doc);
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                BufferedImage image = r.renderImageWithDPI(i, 80);
                ImageIO.write(image, "png", Path.of(base + "-" + (i + 1) + ".png").toFile());
            }
        }
    }
}
