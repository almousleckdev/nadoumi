package com.nadoumi.media.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.nadoumi.common.media.MediaCategory;
import com.nadoumi.media.policy.MediaCategoryPolicy;
import com.nadoumi.media.validation.MediaValidationException.Reason;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

class MediaValidationTest {

    // 8-byte PNG signature + a minimal IHDR chunk.
    private static final byte[] PNG = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4, (byte) 0x89
    };
    private static final byte[] PDF = "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\n"
            .getBytes(StandardCharsets.US_ASCII);
    private static final byte[] SVG = "<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\"/>"
            .getBytes(StandardCharsets.US_ASCII);
    private static final byte[] HTML = "<!DOCTYPE html><html><body>hi</body></html>"
            .getBytes(StandardCharsets.US_ASCII);
    private static final byte[] EXE = { 0x4D, 0x5A, (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00 };

    private static InputStream head(byte[] bytes) {
        return new ByteArrayInputStream(bytes);
    }

    private static Reason reasonOf(ThrowingCallable call) {
        try {
            call.call();
        } catch (MediaValidationException e) {
            return e.reason();
        } catch (Throwable t) {
            throw new AssertionError("expected MediaValidationException, got " + t, t);
        }
        return fail("expected MediaValidationException, none thrown");
    }

    @Test
    void rejectsOversize() {
        long max = MediaCategoryPolicy.of(MediaCategory.UNIVERSITY_LOGO).maxBytes();
        assertThat(reasonOf(() -> MediaValidation.check(
                head(PNG), "image/png", "logo.png", max + 1, MediaCategory.UNIVERSITY_LOGO)))
                .isEqualTo(Reason.TOO_LARGE);
    }

    @Test
    void rejectsDisallowedDeclaredType() {
        assertThat(reasonOf(() -> MediaValidation.check(
                head(HTML), "text/html", "x.html", HTML.length, MediaCategory.UNIVERSITY_LOGO)))
                .isEqualTo(Reason.DISALLOWED_TYPE);
    }

    @Test
    void rejectsSvg() {
        assertThat(reasonOf(() -> MediaValidation.check(
                head(SVG), "image/svg+xml", "x.svg", SVG.length, MediaCategory.UNIVERSITY_LOGO)))
                .isEqualTo(Reason.DISALLOWED_TYPE);
    }

    @Test
    void rejectsHtml() {
        assertThat(reasonOf(() -> MediaValidation.check(
                head(HTML), "text/html", "x.html", HTML.length, MediaCategory.PROGRAM_IMAGE)))
                .isEqualTo(Reason.DISALLOWED_TYPE);
    }

    @Test
    void rejectsExecutable() {
        assertThat(reasonOf(() -> MediaValidation.check(
                head(EXE), "application/x-msdownload", "x.exe", EXE.length, MediaCategory.APPLICATION_DOCUMENT)))
                .isEqualTo(Reason.DISALLOWED_TYPE);
    }

    @Test
    void rejectsDeclaredImageButSniffedPdf() {
        assertThat(reasonOf(() -> MediaValidation.check(
                head(PDF), "image/png", "photo.png", PDF.length, MediaCategory.UNIVERSITY_LOGO)))
                .isEqualTo(Reason.TYPE_MISMATCH);
    }

    @Test
    void acceptsRealPng() {
        var v = MediaValidation.check(
                head(PNG), "image/png", "logo.png", PNG.length, MediaCategory.UNIVERSITY_LOGO);
        assertThat(v.resolvedContentType()).isEqualTo("image/png");
        assertThat(v.sanitizedFilename()).isEqualTo("logo.png");
    }

    @Test
    void acceptsRealPdfForDocument() {
        var v = MediaValidation.check(
                head(PDF), "application/pdf", "cv.pdf", PDF.length, MediaCategory.APPLICATION_DOCUMENT);
        assertThat(v.resolvedContentType()).isEqualTo("application/pdf");
        assertThat(v.sanitizedFilename()).isEqualTo("cv.pdf");
    }

    @Test
    void sanitizesFilename() {
        var v = MediaValidation.check(
                head(PNG), "image/png", "../../etc/passwd .png", PNG.length, MediaCategory.UNIVERSITY_LOGO);
        assertThat(v.sanitizedFilename()).isEqualTo("etc_passwd.png");
    }

    @Test
    void blankFilenameGetsGenerated() {
        var v = MediaValidation.check(
                head(PNG), "image/png", "", PNG.length, MediaCategory.UNIVERSITY_LOGO);
        assertThat(v.sanitizedFilename()).matches("^upload-[0-9a-f-]{36}\\.png$");
    }

    // ---- Office documents as chat attachments (a real .docx is a ZIP; tika-core alone sniffs it as application/zip) ----

    private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation";

    /** A minimal but structurally real OOXML package: [Content_Types].xml first, then the main part. */
    private static byte[] ooxml(String mainPartName, String mainContentType) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(out)) {
            String types = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                    + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                    + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                    + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                    + "<Override PartName=\"/" + mainPartName + "\" ContentType=\"" + mainContentType + "\"/></Types>";
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write(types.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("_rels/.rels"));
            zip.write("<Relationships/>".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry(mainPartName));
            zip.write("<root/>".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.finish();
            return out.toByteArray();
        }
        catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static byte[] docxBytes() {
        return ooxml("word/document.xml", "application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml");
    }

    @Test
    void acceptsARealWordDocumentAsAMessageAttachment() {
        byte[] docx = docxBytes();

        var ok = MediaValidation.check(head(docx), DOCX, "cv.docx", docx.length, MediaCategory.MESSAGE_ATTACHMENT);

        assertThat(ok.resolvedContentType()).isEqualTo(DOCX);
        assertThat(ok.sanitizedFilename()).isEqualTo("cv.docx");
    }

    @Test
    void acceptsRealExcelAndPowerPointFiles() {
        byte[] xlsx = ooxml("xl/workbook.xml", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml");
        byte[] pptx = ooxml("ppt/presentation.xml", "application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml");

        assertThat(MediaValidation.check(head(xlsx), XLSX, "grades.xlsx", xlsx.length, MediaCategory.MESSAGE_ATTACHMENT)
                .resolvedContentType()).isEqualTo(XLSX);
        assertThat(MediaValidation.check(head(pptx), PPTX, "slides.pptx", pptx.length, MediaCategory.MESSAGE_ATTACHMENT)
                .resolvedContentType()).isEqualTo(PPTX);
    }

    @Test
    void rejectsAPlainZipPassedOffAsAWordDocument() {
        byte[] zip = ooxml("notes/readme.txt", "text/plain"); // a zip with no word/ part

        assertThat(reasonOf(() -> MediaValidation.check(head(zip), DOCX, "cv.docx", zip.length, MediaCategory.MESSAGE_ATTACHMENT)))
                .isIn(Reason.TYPE_MISMATCH, Reason.DISALLOWED_TYPE);
    }

    @Test
    void rejectsAWordPackageClaimingToBeASpreadsheet() {
        byte[] docx = docxBytes();

        assertThat(reasonOf(() -> MediaValidation.check(head(docx), XLSX, "sheet.xlsx", docx.length, MediaCategory.MESSAGE_ATTACHMENT)))
                .isEqualTo(Reason.TYPE_MISMATCH);
    }

    @Test
    void rejectsAnExecutableRenamedAsADocument() {
        assertThat(reasonOf(() -> MediaValidation.check(head(EXE), DOCX, "cv.docx", EXE.length, MediaCategory.MESSAGE_ATTACHMENT)))
                .isIn(Reason.TYPE_MISMATCH, Reason.DISALLOWED_TYPE);
    }

    @Test
    void acceptsPlainText_butNotHtmlRenamedAsText() {
        byte[] txt = "Dear advisor, please find my notes.\n".getBytes(StandardCharsets.UTF_8);

        assertThat(MediaValidation.check(head(txt), "text/plain", "notes.txt", txt.length, MediaCategory.MESSAGE_ATTACHMENT)
                .resolvedContentType()).isEqualTo("text/plain");
        assertThat(reasonOf(() -> MediaValidation.check(head(HTML), "text/plain", "notes.txt", HTML.length, MediaCategory.MESSAGE_ATTACHMENT)))
                .isIn(Reason.TYPE_MISMATCH, Reason.DISALLOWED_TYPE);
    }

    @Test
    void keepsTheOtherCategoriesNarrow() {
        byte[] docx = docxBytes();

        assertThat(reasonOf(() -> MediaValidation.check(head(docx), DOCX, "cv.docx", docx.length, MediaCategory.OTHER_ATTACHMENT)))
                .isEqualTo(Reason.DISALLOWED_TYPE);
    }

    @Test
    void acceptsALegacyWordDocument_byItsOle2Signature_butNotWhenClaimedAsAnImage() {
        byte[] doc = new byte[512];
        byte[] signature = { (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1 };
        System.arraycopy(signature, 0, doc, 0, signature.length);

        assertThat(MediaValidation.check(head(doc), "application/msword", "old.doc", doc.length, MediaCategory.MESSAGE_ATTACHMENT)
                .resolvedContentType()).isEqualTo("application/msword");
        assertThat(reasonOf(() -> MediaValidation.check(head(doc), "image/png", "old.png", doc.length, MediaCategory.MESSAGE_ATTACHMENT)))
                .isEqualTo(Reason.TYPE_MISMATCH);
    }

    @Test
    void rejectsAMacroEnabledWordPackage() {
        byte[] docm = ooxml("word/document.xml", "application/vnd.ms-word.document.macroEnabled.main+xml");

        assertThat(reasonOf(() -> MediaValidation.check(head(docm), DOCX, "macro.docx", docm.length, MediaCategory.MESSAGE_ATTACHMENT)))
                .isIn(Reason.TYPE_MISMATCH, Reason.DISALLOWED_TYPE);
    }
}
