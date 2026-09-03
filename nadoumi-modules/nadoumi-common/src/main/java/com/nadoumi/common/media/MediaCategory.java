package com.nadoumi.common.media;

/**
 * Business purpose of a media asset. Each category maps (in {@code nadoumi-media})
 * to a default access class, an allowed MIME set, a max size and a provider folder
 * (Part I, §I.5 / §I.6).
 */
public enum MediaCategory {
    UNIVERSITY_LOGO,
    UNIVERSITY_BANNER,
    UNIVERSITY_GALLERY,
    SCHOLARSHIP_HERO,
    SCHOLARSHIP_COVER,
    PROGRAM_IMAGE,
    APPLICANT_PHOTO,
    APPLICANT_DOCUMENT,
    APPLICATION_DOCUMENT,
    ADMISSION_DOCUMENT,
    JW202,
    OTHER_ATTACHMENT
}
