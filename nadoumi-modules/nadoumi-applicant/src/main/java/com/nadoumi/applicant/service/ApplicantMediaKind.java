package com.nadoumi.applicant.service;

import com.nadoumi.applicant.domain.Applicant;
import com.nadoumi.common.media.MediaCategory;
import java.util.function.Function;

/** The protected files an applicant keeps: each has a media category and a slot on the applicant row. */
public enum ApplicantMediaKind {

    PHOTO(MediaCategory.APPLICANT_PHOTO, Applicant::getPhotoMediaId),
    PASSPORT(MediaCategory.APPLICANT_PASSPORT, Applicant::getPassportMediaId);

    private final MediaCategory category;
    private final Function<Applicant, Long> mediaId;

    ApplicantMediaKind(MediaCategory category, Function<Applicant, Long> mediaId) {
        this.category = category;
        this.mediaId = mediaId;
    }

    public MediaCategory category() {
        return category;
    }

    /** The stored media id for this kind, or null when nothing has been uploaded. */
    public Long mediaIdOf(Applicant applicant) {
        return mediaId.apply(applicant);
    }
}
