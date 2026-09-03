package com.nadoumi.media.policy;

import com.nadoumi.common.media.MediaAccessClass;
import com.nadoumi.common.media.MediaCategory;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Static per-category upload policy — the authoritative transcription of spec
 * §I.5: default access class, MIME allow-list, size cap, Cloudinary
 * {@code resource_type} and folder suffix for every {@link MediaCategory}.
 *
 * <p>No Spring. The rule table is an {@link EnumMap} built once in a static
 * initializer, which also asserts that every enum member has a rule.</p>
 */
public final class MediaCategoryPolicy {

    private MediaCategoryPolicy() {
    }

    /** One binary megabyte. All size caps are expressed as a small multiple of this. */
    private static final long MB = 1024L * 1024L;

    /** Cloudinary {@code resource_type} for image-only categories. */
    private static final String RESOURCE_IMAGE = "image";

    /**
     * Cloudinary {@code resource_type} for document categories. The spec lists
     * "raw / image"; {@code raw} is the stored default and a later task chooses
     * per file.
     */
    private static final String RESOURCE_RAW = "raw";

    /** Allow-list shared by every image category. */
    private static final Set<String> IMAGE_MIME = Set.of("image/jpeg", "image/png", "image/webp");

    /** Allow-list shared by every document / attachment category. */
    private static final Set<String> DOCUMENT_MIME = Set.of("application/pdf", "image/jpeg", "image/png");

    /**
     * Resolved policy for one {@link MediaCategory}.
     *
     * @param defaultAccessClass access class applied when the upload command does not override it
     * @param allowedMime        immutable set of accepted content types
     * @param maxBytes           hard size cap for this category
     * @param resourceType       Cloudinary {@code resource_type} ({@code image} | {@code raw})
     * @param folderSuffix       path appended after {@code nadoumi/<env>/}
     */
    public record CategoryRule(
            MediaAccessClass defaultAccessClass,
            Set<String> allowedMime,
            long maxBytes,
            String resourceType,
            String folderSuffix) {

        public CategoryRule {
            allowedMime = Set.copyOf(allowedMime);
        }
    }

    private static final Map<MediaCategory, CategoryRule> RULES = new EnumMap<>(MediaCategory.class);

    static {
        RULES.put(MediaCategory.UNIVERSITY_LOGO, new CategoryRule(
                MediaAccessClass.PUBLIC, IMAGE_MIME, 4 * MB, RESOURCE_IMAGE, "university/logo"));
        RULES.put(MediaCategory.UNIVERSITY_BANNER, new CategoryRule(
                MediaAccessClass.PUBLIC, IMAGE_MIME, 8 * MB, RESOURCE_IMAGE, "university/banner"));
        RULES.put(MediaCategory.UNIVERSITY_GALLERY, new CategoryRule(
                MediaAccessClass.PUBLIC, IMAGE_MIME, 8 * MB, RESOURCE_IMAGE, "university/gallery"));
        RULES.put(MediaCategory.SCHOLARSHIP_HERO, new CategoryRule(
                MediaAccessClass.PUBLIC, IMAGE_MIME, 8 * MB, RESOURCE_IMAGE, "scholarship/hero"));
        RULES.put(MediaCategory.SCHOLARSHIP_COVER, new CategoryRule(
                MediaAccessClass.PUBLIC, IMAGE_MIME, 6 * MB, RESOURCE_IMAGE, "scholarship/cover"));
        RULES.put(MediaCategory.PROGRAM_IMAGE, new CategoryRule(
                MediaAccessClass.PUBLIC, IMAGE_MIME, 6 * MB, RESOURCE_IMAGE, "program"));
        RULES.put(MediaCategory.APPLICANT_PHOTO, new CategoryRule(
                MediaAccessClass.PROTECTED, IMAGE_MIME, 5 * MB, RESOURCE_IMAGE, "applicant/photo"));
        RULES.put(MediaCategory.APPLICANT_DOCUMENT, new CategoryRule(
                MediaAccessClass.PROTECTED, DOCUMENT_MIME, 20 * MB, RESOURCE_RAW, "applicant/doc"));
        RULES.put(MediaCategory.APPLICATION_DOCUMENT, new CategoryRule(
                MediaAccessClass.PROTECTED, DOCUMENT_MIME, 20 * MB, RESOURCE_RAW, "application/doc"));
        RULES.put(MediaCategory.ADMISSION_DOCUMENT, new CategoryRule(
                MediaAccessClass.PROTECTED, DOCUMENT_MIME, 20 * MB, RESOURCE_RAW, "application/admission"));
        RULES.put(MediaCategory.JW202, new CategoryRule(
                MediaAccessClass.SENSITIVE, DOCUMENT_MIME, 20 * MB, RESOURCE_RAW, "application/jw202"));
        RULES.put(MediaCategory.OTHER_ATTACHMENT, new CategoryRule(
                MediaAccessClass.PROTECTED, DOCUMENT_MIME, 20 * MB, RESOURCE_RAW, "application/other"));

        for (MediaCategory category : MediaCategory.values()) {
            if (!RULES.containsKey(category)) {
                throw new IllegalStateException("MediaCategoryPolicy has no rule for " + category);
            }
        }
    }

    /** The rule for {@code category}. Never null — every category is mapped. */
    public static CategoryRule of(MediaCategory category) {
        CategoryRule rule = RULES.get(category);
        if (rule == null) {
            throw new IllegalArgumentException("Unknown media category: " + category);
        }
        return rule;
    }

    /** Cloudinary folder for {@code category} in {@code env}: {@code nadoumi/<env>/<folderSuffix>}. */
    public static String folder(MediaCategory category, String env) {
        return "nadoumi/" + env + "/" + of(category).folderSuffix();
    }
}
