package com.nadoumi.application.domain.enums;

import com.nadoumi.common.media.MediaCategory;

/** The two documents released to the student after the service fee. */
public enum LetterKind {
    ADMISSION_LETTER(MediaCategory.ADMISSION_DOCUMENT),
    JW202(MediaCategory.JW202);

    private final MediaCategory mediaCategory;

    LetterKind(MediaCategory mediaCategory) {
        this.mediaCategory = mediaCategory;
    }

    public MediaCategory mediaCategory() {
        return mediaCategory;
    }
}
