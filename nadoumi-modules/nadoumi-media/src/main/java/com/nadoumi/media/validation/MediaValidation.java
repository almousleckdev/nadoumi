package com.nadoumi.media.validation;

import com.nadoumi.common.media.MediaCategory;
import com.nadoumi.media.policy.MediaCategoryPolicy;
import com.nadoumi.media.policy.MediaCategoryPolicy.CategoryRule;
import com.nadoumi.media.validation.MediaValidationException.Reason;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.nio.charset.StandardCharsets;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

/**
 * Stateless upload boundary validator (spec §I.5). One entry point,
 * {@link #check}, run by every upload controller before any Cloudinary call.
 *
 * <p>Order of checks: (1) size cap, (2) filename sanitisation, (3) declared
 * content type against the category allow-list, (4) Tika magic-byte sniff of the
 * first {@value #SNIFF_WINDOW_BYTES} bytes — the sniffed type must be allow-listed
 * and share a family (image/pdf) with the declared type, (5) a hard denylist that
 * applies regardless of category. Checksum is out of scope here; the service
 * computes it while streaming to the provider.</p>
 */
public final class MediaValidation {

    private MediaValidation() {
    }

    /** Bytes read from the head of the stream for content sniffing. */
    private static final int SNIFF_WINDOW_BYTES = 8 * 1024;

    /** Upper bound on the sanitised filename. */
    private static final int MAX_FILENAME_LENGTH = 100;

    /** Guard against pathological raw filename input before regex work. */
    private static final int MAX_RAW_FILENAME_LENGTH = 1024;

    private static final String GENERATED_NAME_PREFIX = "upload-";

    /** Never accepted, whatever the category claims — active-content and archive types. */
    private static final Set<String> HARD_DENYLIST = Set.of(
            "text/html",
            "image/svg+xml",
            "application/xhtml+xml",
            "application/x-msdownload",
            "application/x-sh",
            "application/zip",
            "application/java-archive");

    private static final Map<String, String> EXTENSION_BY_TYPE = Map.of(
            "image/png", "png",
            "image/jpeg", "jpg",
            "image/webp", "webp",
            "application/pdf", "pdf");

    private static final Detector DETECTOR = new DefaultDetector();

    /** Outcome of a passing validation: the safe filename and the canonical (sniffed) content type. */
    public record ValidatedUpload(String sanitizedFilename, String resolvedContentType) {
    }

    /**
     * Validate one upload. Consumes up to {@value #SNIFF_WINDOW_BYTES} bytes of
     * {@code head}.
     *
     * @throws MediaValidationException with a {@link Reason} on the first failing check
     */
    public static ValidatedUpload check(InputStream head,
                                        String declaredContentType,
                                        String originalFilename,
                                        long byteSize,
                                        MediaCategory category) {
        CategoryRule rule = MediaCategoryPolicy.of(category);

        // (1) size
        if (byteSize > rule.maxBytes()) {
            throw new MediaValidationException(Reason.TOO_LARGE,
                    "upload is " + byteSize + " bytes; " + category + " allows " + rule.maxBytes());
        }

        // (2) filename sanitisation (blank is filled in after the type is resolved)
        String sanitized = sanitizeFilename(originalFilename);

        // (3) declared type allow-list (denylist folded in — a denied type is never allow-listed)
        String declared = normalize(declaredContentType);
        if (declared.isEmpty() || HARD_DENYLIST.contains(declared) || !rule.allowedMime().contains(declared)) {
            throw new MediaValidationException(Reason.DISALLOWED_TYPE,
                    "declared content type '" + declaredContentType + "' is not accepted for " + category);
        }

        // (4) magic-byte sniff — the resolved type is canonical, not the client's claim
        String resolved = sniff(head, declared);

        // (5) hard denylist on the sniffed type
        if (HARD_DENYLIST.contains(resolved)) {
            throw new MediaValidationException(Reason.DISALLOWED_TYPE,
                    "sniffed content type '" + resolved + "' is on the hard denylist");
        }
        if (!rule.allowedMime().contains(resolved)) {
            throw new MediaValidationException(Reason.TYPE_MISMATCH,
                    "sniffed content type '" + resolved + "' is not accepted for " + category);
        }
        if (!family(declared).equals(family(resolved))) {
            throw new MediaValidationException(Reason.TYPE_MISMATCH,
                    "declared '" + declared + "' but content is '" + resolved + "'");
        }

        if (sanitized.isEmpty()) {
            sanitized = GENERATED_NAME_PREFIX + UUID.randomUUID() + "." + extensionFor(resolved);
        }
        return new ValidatedUpload(sanitized, resolved);
    }

    private static String sanitizeFilename(String raw) {
        if (raw == null) {
            return "";
        }
        if (raw.length() > MAX_RAW_FILENAME_LENGTH) {
            throw new MediaValidationException(Reason.BAD_NAME, "filename is too long");
        }
        String s = raw.replace('\\', '/');
        s = s.replaceAll("\\p{Cntrl}", "");
        s = s.replace('/', '_');
        s = s.replaceAll("^[._-]+", "");
        s = s.replaceAll("[^A-Za-z0-9._-]", "");
        if (s.length() > MAX_FILENAME_LENGTH) {
            s = s.substring(0, MAX_FILENAME_LENGTH);
        }
        return s;
    }

    private static String sniff(InputStream head, String declared) {
        try {
            byte[] window = head.readNBytes(SNIFF_WINDOW_BYTES);
            Metadata metadata = new Metadata();
            try (TikaInputStream tis = TikaInputStream.get(window, metadata)) {
                MediaType type = DETECTOR.detect(tis, metadata);
                return refineOffice(type.getBaseType().toString(), window, declared);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read upload head for sniffing", e);
        }
    }

    /** What tika-core reports for any OOXML package, and for any legacy OLE2 (.doc/.xls/.ppt) file. */
    private static final String TIKA_OOXML = "application/x-tika-ooxml";
    private static final String TIKA_OLE2 = "application/x-tika-msoffice";
    private static final String MSWORD = "application/msword";

    /** Marker in an OOXML package's {@code [Content_Types].xml} -> the concrete type. Macro-enabled variants have no entry. */
    private static final Map<String, String> OOXML_MAIN_PARTS = Map.of(
            "wordprocessingml.document.main+xml",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "spreadsheetml.sheet.main+xml",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "presentationml.presentation.main+xml",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation");

    /**
     * tika-core alone cannot tell a Word file from any other ZIP or OLE2 container. Office files are therefore
     * identified from their own content: an OOXML package by the main part its first entry declares, a legacy
     * OLE2 file by its signature plus the client's .doc claim. Anything else keeps the sniffed type and is
     * judged (and normally refused) by the allow-list.
     */
    private static String refineOffice(String sniffed, byte[] window, String declared) {
        if (TIKA_OOXML.equals(sniffed)) {
            String main = contentTypesOf(window);
            for (Map.Entry<String, String> part : OOXML_MAIN_PARTS.entrySet()) {
                if (main.contains(part.getKey())) {
                    return part.getValue();
                }
            }
            return sniffed;
        }
        return TIKA_OLE2.equals(sniffed) && MSWORD.equals(declared) ? MSWORD : sniffed;
    }

    /** The text of the package's {@code [Content_Types].xml} (its first entry), or empty when unreadable. */
    private static String contentTypesOf(byte[] window) {
        try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(window))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if ("[Content_Types].xml".equals(entry.getName())) {
                    return new String(zip.readNBytes(SNIFF_WINDOW_BYTES), StandardCharsets.UTF_8);
                }
            }
        }
        catch (IOException | RuntimeException e) {
            // a truncated or malformed package: left to the allow-list, which will refuse it
        }
        return "";
    }

    private static String family(String mime) {
        if (mime.startsWith("image/")) {
            return "image";
        }
        if (mime.equals("application/pdf")) {
            return "pdf";
        }
        return mime;
    }

    private static String extensionFor(String mime) {
        String ext = EXTENSION_BY_TYPE.get(mime);
        if (ext != null) {
            return ext;
        }
        int slash = mime.indexOf('/');
        return slash >= 0 ? mime.substring(slash + 1) : "bin";
    }

    private static String normalize(String contentType) {
        if (contentType == null) {
            return "";
        }
        int semicolon = contentType.indexOf(';');
        String base = semicolon >= 0 ? contentType.substring(0, semicolon) : contentType;
        return base.trim().toLowerCase();
    }
}
