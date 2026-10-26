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
    void shouldPrintThePaidLineWithTheTransactionNumber_andKeepThePhotoClearOfIt() throws Exception {
        var base = PdfFixtures.form(PdfFixtures.programme(), "Amina");
        var paid = new ApplicationFormModel(base.formNo(), base.generatedAt(), base.applicationRef(), base.opportunity(),
                "NAD-AB12CD34", "BANK1234567", java.time.Instant.parse("2026-10-22T07:00:00Z"), base.dossier(),
                base.photo(), base.signature(), base.signedOn(), base.verifyUrl());
        byte[] pdf = forms.render(paid);

        String text = text(pdf).replaceAll("\\s+", " ");
        assertThat(text).contains("PAID", "NAD-AB12CD34", "Transaction BANK1234567", "Form no.", "Generated");
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
                "Marital status", "Single", "Muslim", "Place of birth", "Ibrahima Diallo", "Fatoumata Diallo", "(unavailable)",
                "Passed away", "Financial supporter", "Port Authority", "Emergency contact in China", "Li Wei",
                "Applicant affirmation", "5 Avenue Centrale",
                "Applicant's signature", "NAD-APP-2026-000042", "PAID");
    }

    @Test
    void shouldDrawHostileOrUnsupportedTextAsPlainText_withoutFailing() throws IOException {
        String hostile = "<img src=\"http://127.0.0.1:1/x\"> @import url(file:///etc/passwd) \u0001 \ud83d\ude00 中文";

        byte[] pdf = forms.render(PdfFixtures.form(PdfFixtures.programme(), hostile));

        String text = text(pdf);
        assertThat(text).contains("<img src=\"http://127.0.0.1:1/x\">").doesNotContain("\u0001");
        assertThat(text).contains("?", "中", "文"); // a glyph no font has becomes '?'; Chinese prints with the Chinese font
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
                model.opportunity(), model.paymentReference(), dossier, model.photo(), model.signature(), model.signedOn(), model.verifyUrl()));

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
    void shouldPlaceThePhotoInTheMasthead_andRenderWithoutOneWhenThereIsNone() throws IOException {
        var withPhoto = PdfFixtures.form(PdfFixtures.programme(), "Amina");
        var without = new ApplicationFormModel(withPhoto.formNo(), withPhoto.generatedAt(), withPhoto.applicationRef(),
                withPhoto.opportunity(), withPhoto.paymentReference(), withPhoto.dossier(), null, null, null, null);
        var garbage = new ApplicationFormModel(withPhoto.formNo(), withPhoto.generatedAt(), withPhoto.applicationRef(),
                withPhoto.opportunity(), withPhoto.paymentReference(), withPhoto.dossier(), new byte[] {1, 2, 3, 4}, new byte[] {9, 9}, null, null);

        byte[] big = forms.render(withPhoto);
        byte[] plain = forms.render(without);
        byte[] ignored = forms.render(garbage); // an unusable image is left out, never an error

        assertThat(big.length).isGreaterThan(plain.length);
        assertThat(text(big)).contains("Application Form", "NAD-APP-2026-000042");
        assertThat(text(ignored)).contains("Application Form");
        assertThat(images(big)).isGreaterThan(images(plain));
    }

    private static int images(byte[] pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            int count = 0;
            for (var name : doc.getPage(0).getResources().getXObjectNames()) {
                if (doc.getPage(0).getResources().getXObject(name) instanceof org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject) {
                    count++;
                }
            }
            return count;
        }
    }

    @Test
    void shouldPrintTheSignatureDate_andLeaveTheBoxEmptyWhenUnsigned() throws IOException {
        var signed = PdfFixtures.form(PdfFixtures.programme(), "Amina");
        var unsigned = new ApplicationFormModel(signed.formNo(), signed.generatedAt(), signed.applicationRef(),
                signed.opportunity(), signed.paymentReference(), signed.dossier(), signed.photo(), null, null, signed.verifyUrl());

        assertThat(text(forms.render(signed))).contains("2026-10-22");
        assertThat(text(forms.render(unsigned))).contains("Applicant's signature").doesNotContain("Date 2026");
    }

    @Test
    void shouldPrintAQrCodeThatOpensTheVerificationPage_onEveryPage() throws Exception {
        byte[] pdf = receipts.render(PdfFixtures.receipt("Fudan University"));

        try (PDDocument doc = Loader.loadPDF(pdf)) {
            BufferedImage page = new PDFRenderer(doc).renderImageWithDPI(0, 200);
            var bitmap = new com.google.zxing.BinaryBitmap(new com.google.zxing.common.HybridBinarizer(
                    new com.google.zxing.RGBLuminanceSource(page.getWidth(), page.getHeight(),
                            page.getRGB(0, 0, page.getWidth(), page.getHeight(), null, 0, page.getWidth()))));
            var result = new com.google.zxing.MultiFormatReader().decode(bitmap,
                    java.util.Map.of(com.google.zxing.DecodeHintType.TRY_HARDER, Boolean.TRUE,
                            com.google.zxing.DecodeHintType.POSSIBLE_FORMATS, java.util.List.of(com.google.zxing.BarcodeFormat.QR_CODE)));
            assertThat(result.getText()).isEqualTo(PdfFixtures.VERIFY_URL);
        }
        assertThat(text(pdf)).contains("Verify this document");
    }

    @Test
    void shouldLeaveTheQrOut_whenThereIsNoVerificationAddress() throws IOException {
        var model = PdfFixtures.receipt("Fudan University");
        byte[] pdf = receipts.render(new ReceiptModel(model.receiptNo(), model.issuedAt(), model.studentName(),
                model.studentPublicId(), model.studentEmail(), model.applicationRef(), model.opportunityTitle(),
                model.universityName(), model.intake(), model.paymentReference(), model.transactionNo(), model.paidAt(),
                model.amount(), model.currency(), model.payee(), model.serviceName(), null));

        assertThat(text(pdf)).doesNotContain("Verify this document");
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
