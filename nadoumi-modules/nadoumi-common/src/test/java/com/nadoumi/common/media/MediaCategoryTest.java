package com.nadoumi.common.media;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class MediaCategoryTest {

    @Test
    void everyCategoryIsCoveredByTheDocumentedSet() {
        assertThat(Arrays.stream(MediaCategory.values()).map(Enum::name))
                .containsExactlyInAnyOrder(
                        "UNIVERSITY_LOGO", "UNIVERSITY_BANNER", "UNIVERSITY_GALLERY",
                        "SCHOLARSHIP_HERO", "SCHOLARSHIP_COVER", "PROGRAM_IMAGE",
                        "APPLICANT_PHOTO", "APPLICANT_DOCUMENT", "APPLICATION_DOCUMENT",
                        "ADMISSION_DOCUMENT", "JW202", "OTHER_ATTACHMENT");
    }

    @Test
    void accessClassesAreThreeTiered() {
        assertThat(MediaAccessClass.values()).containsExactly(
                MediaAccessClass.PUBLIC, MediaAccessClass.PROTECTED, MediaAccessClass.SENSITIVE);
    }
}
