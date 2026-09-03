package com.nadoumi.media.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.nadoumi.common.media.MediaCategory;
import com.nadoumi.media.policy.MediaCategoryPolicy;
import com.nadoumi.media.validation.MediaValidationException.Reason;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
}
